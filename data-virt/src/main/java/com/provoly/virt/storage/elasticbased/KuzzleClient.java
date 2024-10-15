package com.provoly.virt.storage.elasticbased;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.storage.InsertionError;
import com.provoly.virt.storage.elasticbased.kuzzle.KuzzleNotifierService;
import com.provoly.virt.storage.elasticbased.kuzzle.KuzzleUnconfiguredClient;

import io.kuzzle.sdk.Kuzzle;
import io.kuzzle.sdk.coreClasses.SearchResult;
import io.kuzzle.sdk.coreClasses.maps.KuzzleMap;
import io.kuzzle.sdk.events.LoginAttemptEvent;
import io.kuzzle.sdk.events.NetworkStateChangeEvent;
import io.kuzzle.sdk.protocol.ProtocolState;
import io.kuzzle.sdk.protocol.WebSocket;
import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;

import kotlin.Unit;

@ApplicationScoped
public class KuzzleClient {

    public static final String REPLICAS_PROPERTY = "number_of_replicas";
    public static final String SETTINGS = "settings";
    public static final String AUTH_STRATEGY = "local";
    public static final int DEFAULT_PORT = 7512;
    public static final int DEFAULT_REPLICAS = 1;
    private static final String JWT_PROPERTY = "jwt";
    private static final int SECONDS_TO_REFRESH_TOKEN_BEFORE_IT_EXPIRES = 120;
    private final Logger log;
    private Kuzzle kuzzle;
    private final Optional<DataVirtProperties.KuzzleConfiguration> kuzzleConfiguration;
    private final KuzzleNotifierService kuzzleNotifierService;
    private Thread reconnectThread = null;
    private int replicas;
    private String token;

    public KuzzleClient(DataVirtProperties config, Logger log, KuzzleNotifierService kuzzleNotifierService) {
        this.log = log;
        this.kuzzleConfiguration = config.kuzzle();
        this.kuzzleNotifierService = kuzzleNotifierService;
        replicas = kuzzleConfiguration.flatMap(DataVirtProperties.KuzzleConfiguration::replicas)
                .orElse(DEFAULT_REPLICAS);
    }

    /*
     * *
     * Kuzzle connection is initialized when application starts.
     * Liveness and readiness are available when startup is finished.
     * this.kuzzle is obviously initialized.
     */
    void initKuzzleClient(@Observes StartupEvent ev) {
        kuzzleConfiguration.ifPresent(config -> {
            log.info("Initializing Kuzzle client");

            WebSocket ws = new WebSocket(config.host(), config.port().orElse(DEFAULT_PORT), false,
                    false);
            this.kuzzle = new Kuzzle(ws);

            addNetworkChangeListener(config, ws);
            addLoginListener(ws);

            startReconnectThreadToKuzzle(log, config);
            startKeepAlive();
        });
    }

    public Kuzzle client() {
        if (kuzzle == null) {
            throw new KuzzleUnconfiguredClient();
        }
        return kuzzle;
    }

    public boolean isConfigured() {
        return kuzzleConfiguration.isPresent();
    }

    public String getTenantName() {
        return kuzzleConfiguration.map(DataVirtProperties.KuzzleConfiguration::tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.TECHNICAL,
                        "Tenant property is mandatory to use Kuzzle device manager"));
    }

    public SearchResult kuzzleSearch(String index, String collection, Map<String, Object> query, int limit) {
        try {
            return client().getDocumentController().search(
                    index,
                    collection,
                    query,
                    null,
                    Integer.valueOf(limit)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }
    }

    public void createIndexAndCollection(String indexName, String collection, Map<String, Map<String, Object>> mapping) {
        try {
            mapping.put(SETTINGS, Map.of(REPLICAS_PROPERTY, replicas));
            client().getIndexController().create(indexName).get();
            client().getCollectionController().create(indexName, collection, mapping).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to create index %s".formatted(indexName), e);
        }
    }

    public boolean indexExists(String indexName) {
        try {
            return client().getIndexController().exists(indexName).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to check if index %s exists".formatted(indexName), e);
        }
    }

    @CacheResult(cacheName = "kuzzle-measures-mapping")
    public Map<String, Object> getMeasureMapping() {
        try {
            return (Map<String, Object>) kuzzle
                    .getCollectionController()
                    .getMapping(getTenantName(), MEASURE_COLLECTION)
                    .get().get("properties");
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to get the measure collection mapping", e);
        }
    }

    public List<InsertionError> insertDocuments(String index, String collection, List<Map<String, Object>> documents) {
        try {
            var result = kuzzle.getDocumentController()
                    .mCreateOrReplace(index, collection, new ArrayList<>(documents))
                    .get();
            return convertKuzzleResponseToError(result);
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error while communicating with kuzzle", e);
        }
    }

    private void authenticate(String username, String password) {
        log.infof("Trying to authenticate to Kuzzle");
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        kuzzle.connect();
        try {
            updateToken(kuzzle.getAuthController().login(AUTH_STRATEGY, credentials).get());
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to authenticate to Kuzzle", e);
        }
    }

    private List<InsertionError> convertKuzzleResponseToError(Map<String, ArrayList<Object>> result) {
        var errors = result.get("errors");
        if (errors == null) {
            return List.of();
        }
        var pryErrors = new ArrayList<InsertionError>();
        for (Object error : errors) {
            var kuzzleErrorMap = (KuzzleMap) error;
            log.error("Error while inserting document in Kuzzle : %s".formatted(kuzzleErrorMap));
            var documentId = kuzzleErrorMap.getMap("document").getString("_id");
            pryErrors.add(new InsertionError(kuzzleErrorMap.getString("reason"), documentId));
        }
        return pryErrors;
    }

    // Kuzzle reconnect option is not working, so we are implementing our own reconnect mechanism
    // Delete this code when Kuzzle will fix the issue : https://github.com/kuzzleio/sdk-jvm/issues/86
    private void startReconnectThreadToKuzzle(Logger log, DataVirtProperties.KuzzleConfiguration config) {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            log.warn("Reconnect thread already started");
            return;
        }

        log.info("Starting reconnect thread to kuzzle");
        this.reconnectThread = new Thread(() -> {
            while (kuzzle.getProtocol().getState() == ProtocolState.CLOSE) {
                try {
                    Thread.sleep(10000);
                    kuzzle.connect();
                    config.credentials()
                            .ifPresentOrElse(credentials -> authenticate(credentials.username(), credentials.password()),
                                    kuzzleNotifierService::startNotificationService);
                    // If authentication is not configured, the notification service must be called, as it will not be triggered by the listener.
                } catch (Exception e) {
                    log.error("Error while reconnecting to Kuzzle :" + e.getMessage());
                }
            }
            // We are connected, exiting the thread
            this.reconnectThread = null;
        }, "kuzzle-reconnect");

        this.reconnectThread.start();
    }

    // Kuzzle sdk forget to implement keepAlive
    // Temporary code waiting fix for : https://github.com/kuzzleio/sdk-jvm/issues/87
    private void startKeepAlive() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(40 * 1000);
                    switch (kuzzle.getProtocol().getState()) {
                        case CLOSE:
                            log.warn("Kuzzle connection is closed, skipping keep-alive");
                            break;
                        case OPEN:
                            log.trace("Sending keep-alive to kuzzle");
                            if (token != null) {
                                refreshTokenIfNeeded();
                            }
                            var date = kuzzle.getServerController().now().get(10, TimeUnit.SECONDS);
                            log.debugf("Keep-alive sent to Kuzzle %s", date);
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error while sending keep-alive to Kuzzle", e);
                }
            }
        }, "kuzzle-keep-alive").start();
    }

    private void addLoginListener(WebSocket ws) {
        ws.addListener(LoginAttemptEvent.class, //  wait for authentication to be established
                event -> {
                    if (event.getSuccess()) {
                        log.infof("Kuzzle authentication successful.");
                        new Thread(kuzzleNotifierService::startNotificationService).start();
                    }
                    return Unit.INSTANCE;
                });
    }

    private void addNetworkChangeListener(DataVirtProperties.KuzzleConfiguration config, WebSocket ws) {
        ws.addListener(NetworkStateChangeEvent.class, event -> {
            log.info("Network state changed: " + event.getState());
            if (event.getState() == ProtocolState.CLOSE) {
                startReconnectThreadToKuzzle(log, config);
            }
            return Unit.INSTANCE;
        });
    }

    private void refreshTokenIfNeeded() throws InterruptedException, ExecutionException {
        log.trace("Check token validity");
        var validityToken = kuzzle.getAuthController().checkToken(token).get();
        var expiresAt = Long.parseLong(validityToken.get("expiresAt").toString());

        if (Instant.now().plusSeconds(SECONDS_TO_REFRESH_TOKEN_BEFORE_IT_EXPIRES).isAfter(Instant.ofEpochMilli(expiresAt))) {
            log.debug("Token expires in less than 2 min, refresh it");
            updateToken(kuzzle.getAuthController().refreshToken().get());
        }
    }

    private void updateToken(Map<String, Object> loginOrRefresh) {
        var jwt = loginOrRefresh.get(JWT_PROPERTY);
        token = jwt.toString();
    }

}
