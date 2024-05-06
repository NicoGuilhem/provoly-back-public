package com.provoly.virt.storage.elasticbased.kuzzle;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.AttributeMultiValue;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.item.ItemsNotifier;
import com.provoly.virt.storage.elasticbased.KuzzleClient;
import com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout;

import io.kuzzle.sdk.coreClasses.maps.KuzzleMap;
import io.kuzzle.sdk.coreClasses.responses.Response;
import io.kuzzle.sdk.handlers.NotificationHandler;
import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class KuzzleNotifierService implements NotificationHandler {

    private final Logger log;
    private final DataVirtProperties dataVirtProperties;
    private final KuzzleClient kuzzleClient;
    private final KuzzleMeasureLayout measureLayout;
    private final ItemsNotifier notifier;

    public KuzzleNotifierService(Logger log,
            DataVirtProperties dataVirtProperties,
            KuzzleClient kuzzleClient,
            KuzzleMeasureLayout measureLayout,
            ItemsNotifier notifier) {
        this.log = log;
        this.dataVirtProperties = dataVirtProperties;
        this.kuzzleClient = kuzzleClient;
        this.measureLayout = measureLayout;
        this.notifier = notifier;
    }

    // FIXME : Manage resubscribe as Kuzzle can be restarted
    // TODO : Manage exception on subscription

    void onStart(@Observes StartupEvent ev) {

        // Kuzzle client is initialized only on first call
        // We have to be sure to call it only when it is configured
        if (dataVirtProperties.kuzzle().host().isEmpty()) {
            log.info("Kuzzle is not configured, Kuzzle notifier service is disabled");
            return;
        }

        if (!kuzzleClient.storageExists(kuzzleClient.getTenantName())) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Tenant %s not exists".formatted(kuzzleClient.getTenantName()));
        }

        log.infof("Registering to Kuzzle notifications");
        //FIXME: Kuzzle not reentrant, find better solution
        var tmp = kuzzleClient.getMeasureMapping();

        kuzzleClient.client()
                .getRealtimeController()
                .subscribe(kuzzleClient.getTenantName(), MEASURE_COLLECTION, new HashMap<>(), this)
                .whenComplete((aVoid, throwable) -> {
                    log.info("Subscribed to notifications on measures collection");
                    if (throwable != null) {
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
            var item = measureLayout.convertToItem((KuzzleMap) response.getResult());
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
