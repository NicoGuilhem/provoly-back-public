package com.provoly.virt.storage.elasticbased.kuzzle;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.provoly.virt.entity.AttributeMultiValue;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.Item;
import com.provoly.virt.item.ItemsNotifier;
import com.provoly.virt.storage.elasticbased.KuzzleClient;
import com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout;

import io.kuzzle.sdk.coreClasses.maps.KuzzleMap;
import io.kuzzle.sdk.coreClasses.responses.Response;
import io.kuzzle.sdk.events.NetworkStateChangeEvent;
import io.kuzzle.sdk.handlers.NotificationHandler;
import io.kuzzle.sdk.protocol.ProtocolState;
import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

import kotlin.Unit;

@ApplicationScoped
public class KuzzleNotifierService implements NotificationHandler {

    private final Logger log;
    private final KuzzleClient kuzzleClient;
    private final KuzzleMeasureLayout measureLayout;
    private final ItemsNotifier notifier;

    private boolean isSubscribed = false;

    public KuzzleNotifierService(Logger log,
            KuzzleClient kuzzleClient,
            KuzzleMeasureLayout measureLayout,
            ItemsNotifier notifier) {
        this.log = log;
        this.kuzzleClient = kuzzleClient;
        this.measureLayout = measureLayout;
        this.notifier = notifier;
    }

    void onStart(@Observes StartupEvent ev) {
        if (!kuzzleClient.isConfigured()) {
            log.info("Kuzzle is not configured, Kuzzle notifier service is disabled");
            return;
        }

        log.info("Starting Kuzzle notifier service");
        this.kuzzleClient.client().getProtocol().addListener(NetworkStateChangeEvent.class, event -> {
            if (event.getState() == ProtocolState.OPEN) {
                // onKuzzleConnect use KuzzleClient we already are in a Kuzzle listener
                // Kuzzle client is not reentrant we need to move to another thread
                new Thread(this::onKuzzleConnect).start();
            }
            return Unit.INSTANCE; // Needed for Kotlin interop with Java
        });
    }

    private void onKuzzleConnect() {
        log.info("Kuzzle is connected");
        if (isSubscribed) {
            // Subscriptions are persistant even across reconnection
            // We need to subscribe only at the first connection
            return;
        }

        log.infof("Registering to Kuzzle notifications");
        //FIXME: Kuzzle not reentrant, find better solution
        // This line preload measure mapping in quarkus cache
        // When this.run use getMeasureMapping in measureLayout.convertToItem
        // Kuzzle is call in a reentrant way
        var tmp = kuzzleClient.getMeasureMapping();

        kuzzleClient.client()
                .getRealtimeController()
                .subscribe(kuzzleClient.getTenantName(), MEASURE_COLLECTION, new HashMap<>(), this)
                .whenComplete((aVoid, throwable) -> {
                    log.info("Subscribed to notifications on measures collection");
                    if (throwable == null) {
                        this.isSubscribed = true;
                    } else {
                        log.error("Error while subscribing to notifications: " + throwable.getMessage());
                    }
                });
    }

    /**
     * Kuzzle notification handler receiving all new measures
     */
    @Override
    public void run(@NotNull Response response) {
        try {
            log.debugf("New measure received {}", response);
            Item item = measureLayout.convertToItem((KuzzleMap) response.getResult());
            // Item are not read from a user and a security context but received from Kuzzle
            // We have to unlock visibility
            item.getAttributes(AttributeSimpleValue.class).forEach(attribute -> attribute.setVisible(true));
            item.getAttributes(AttributeMultiValue.class)
                    .forEach(attribute -> attribute.getValues().forEach(value -> value.setVisible(true)));
            notifier.notifyItemWrittenToStorage(item);
        } catch (Exception e) {
            log.error("Unable to notify item writed to storage", e);
        }
    }

}
