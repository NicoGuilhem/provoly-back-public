package com.provoly.virt.storage.elasticbased;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.search.mono.MonoMapper;
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
    private static final int SECONDS_TTL_SEARCH_RESULT = 50;
    private static final Duration TOKEN_TTL_DURATION = Duration.ofHours(24); // TOKEN_TTL_DURATION must be greater as the longer batch of data imported
    private static final String TOKEN_TTL_SECONDS = String.format("%ds", TOKEN_TTL_DURATION.getSeconds());
    private static final Duration TOKEN_REFRESH_PERIOD = Duration.ofHours(2); // Must be lower as TOKEN_TTL_DURATION
    private final Logger log;
    private Kuzzle kuzzle;
    private final Optional<DataVirtProperties.KuzzleConfiguration> kuzzleConfiguration;
    private final KuzzleNotifierService kuzzleNotifierService;
    private Instant nextRefreshTime;
    private Thread reconnectThread = null;
    private final int replicas;
    private final MonoMapper mapper;
    private final TtlHashMap<String, SearchResult> searchResultCache = new TtlHashMap<>(
            Duration.ofSeconds(SECONDS_TTL_SEARCH_RESULT));

    public KuzzleClient(DataVirtProperties config, Logger log, KuzzleNotifierService kuzzleNotifierService, MonoMapper mapper) {
        this.log = log;
        this.kuzzleConfiguration = config.kuzzle();
        this.kuzzleNotifierService = kuzzleNotifierService;
        this.mapper = mapper;
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

    public SearchResult kuzzleSearchPagination(String index,
            String collection,
            Map<String, Object> query,
            int limit,
            String searchAfter) {

        try {
            if (searchAfter != null) {
                return getNextSearchResult(searchAfter);
            }

            var searchResult = client().getDocumentController().search(
                    index,
                    collection,
                    query,
                    "%ds".formatted(SECONDS_TTL_SEARCH_RESULT),
                    limit).get();
            searchResultCache.put(searchResult.getScrollId(), searchResult);
            return searchResult;

        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }
    }

    private SearchResult getNextSearchResult(String searchAfter) throws InterruptedException, ExecutionException {
        var searchAfterContext = mapper.map(searchAfter);
        var scrollId = searchAfterContext.pit(); // Hideously used pit, searchAfter should be a string in ItemSearchResult
        var previousSearchResult = searchResultCache.get(scrollId);
        if (previousSearchResult != null) {
            var searchResult = previousSearchResult.next().get();
            if (searchResult == null) { // No more result
                searchResultCache.remove(scrollId);
            } else {
                searchResultCache.put(searchResult.getScrollId(), searchResult);
            }
            return searchResult;
        } else {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Unable to find previous search result, searchAfter is invalid or expired");
        }
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
            waitForConnection();
            var result = kuzzle.getDocumentController()
                    .mCreateOrReplace(index, collection, new ArrayList<>(documents))
                    .get();
            return convertKuzzleResponseToError(result);
        } catch (Exception e) { // Should not happens, but NotConnectedException is not catchable but can be raised
            throw new BusinessException(ErrorCode.TECHNICAL, "Error while communicating with kuzzle", e);
        }
    }

    public ProtocolState getState() {
        if (kuzzle == null) {
            return ProtocolState.CLOSE;
        }
        return kuzzle.getProtocol().getState();
    }

    private void waitForConnection() {
        try {
            var startTime = Instant.now();
            while (kuzzle.getProtocol().getState() != ProtocolState.OPEN) {
                log.info("Waiting for Kuzzle reconnected successfully.");
                if (Duration.between(startTime, Instant.now()).toSeconds() > 60) {
                    log.error("Kuzzle connection timed out after 60 seconds");
                    throw new BusinessException(ErrorCode.TECHNICAL, "Kuzzle connection timed out");
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Thread interrupted while waiting for Kuzzle reconnection", e);
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
                            // Warn : if a batch of data is received by notifier, kuzzle client will not respond until end
                            // Today batch can be long as several minutes, we have to ignore timeout and be sure every call have a timeout set
                            log.trace("Sending keep-alive to kuzzle");
                            refreshTokenIfNeeded();
                            var date = kuzzle.getServerController().now().get(10, TimeUnit.SECONDS);
                            log.debugf("Keep-alive sent to Kuzzle %s", date);
                            break;
                    }
                } catch (TimeoutException e) {
                    // If subscription send a lot of messages, kuzzle is not responding anymore.
                    log.warn("Kuzzle keep-alive timed out");
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
                        log.info("Kuzzle authentication successful -> Registering notification service");
                        new Thread(kuzzleNotifierService::startNotificationService).start();
                    } else {
                        log.errorf("Kuzzle authentication failed");
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

    private void authenticate(String username, String password) {
        try {
            log.infof("Trying to authenticate to Kuzzle");
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);
            kuzzle.connect();
            kuzzle.getAuthController().login(AUTH_STRATEGY, credentials, TOKEN_TTL_SECONDS).get();
            nextRefreshTime = Instant.now().plus(TOKEN_REFRESH_PERIOD);
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to authenticate to Kuzzle", e);
        }
    }

    private void refreshTokenIfNeeded() throws InterruptedException, ExecutionException, TimeoutException {
        log.tracef("Check kuzzle token refresh time. Planned at %d", nextRefreshTime);
        if (nextRefreshTime != null && Instant.now().isAfter(nextRefreshTime)) {
            log.info("Refreshing kuzzle token");
            kuzzle.getAuthController().refreshToken(TOKEN_TTL_SECONDS).get(10, TimeUnit.SECONDS);
            nextRefreshTime = Instant.now().plus(TOKEN_REFRESH_PERIOD);
        }
    }

}
