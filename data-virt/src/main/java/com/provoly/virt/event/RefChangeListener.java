package com.provoly.virt.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.clients.CacheClearer;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.ref.*;
import com.provoly.virt.storage.StorageModelAdapter;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RefChangeListener {

    private Logger log;

    private StorageModelAdapter storageModelAdapter;
    private CacheClearer cacheClearer;
    @Inject
    @Channel(RefChangeEvent.TOPIC_NAME + "-out")
    Emitter<RefChangeEvent> eventEmitter;

    public RefChangeListener(Logger log, CacheClearer cacheClearer,
            StorageModelAdapter storageModelAdapter) {
        this.log = log;
        this.cacheClearer = cacheClearer;
        this.storageModelAdapter = storageModelAdapter;
    }

    @Incoming(RefChangeEvent.TOPIC_NAME)
    public void refEvent(RefChangeEvent event) {
        switch (event) {
            case RefChangeEventClassCreated e -> {
                storageModelAdapter.createOClass(e.getoClassDetails());
                sendEventClassReady(e.getoClassDetails());
            }
            case RefChangeEventClassUpdated e -> {
                log.infof("Invalidate caches for oClass %s because it has been updated", e.getoClassDetails().getId());
                cacheClearer.invalidateOClassCaches(e.getoClassDetails().getId());
                storageModelAdapter.updateOClass(e.getoClassDetails());
            }
            case RefChangeEventClassDeleted e -> {
                log.infof("Invalidate caches for oClass %s because it has been deleted", e.getoClassDetails().getId());
                cacheClearer.invalidateOClassCaches(e.getoClassDetails().getId());
                storageModelAdapter.deleteOClass(e.getoClassDetails());
            }
            case RefChangeEventDatasetVersionDeleted e -> {
                log.infof("Delete dataset version %s", e.getDatasetVersionDto().getId());
                storageModelAdapter.deleteDatasetVersion(e.getDatasetVersionDto(), e.getoClassDetailsDto());
            }
            default -> log.infof("Event received but not used %s", event);
        }
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void sendEventClassReady(OClassDetailsDto detailsDto) {
        log.infof("Sending event class ready %s", detailsDto);
        eventEmitter.send(new RefChangeEventClassReady(detailsDto));
    }
}
