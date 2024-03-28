package com.provoly.ref.event;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.virt.*;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.datasetversion.DatasetVersionMessageService;
import com.provoly.ref.datasetversion.DatasetVersionService;

import io.smallrye.common.annotation.Blocking;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VirtChangeListener {

    private Logger log;

    private DatasetVersionMessageService datasetVersionMessageService;
    private DatasetVersionService datasetVersionService;
    private DatasetVersionMapper datasetVersionMapper;

    public VirtChangeListener(Logger log, DatasetVersionMessageService datasetVersionMessageService,
            DatasetVersionService datasetVersionService, DatasetVersionMapper datasetVersionMapper) {
        this.log = log;
        this.datasetVersionMessageService = datasetVersionMessageService;
        this.datasetVersionService = datasetVersionService;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    @Incoming(VirtChangeEvent.TOPIC_NAME)
    @Blocking
    public void virtEvent(VirtChangeEvent event) {
        log.infof("Receive event %s", event.getType());
        switch (event) {
            case VirtChangeEventImportMessageCreated e -> datasetVersionMessageService.save(e.getImportsMessage());
            case VirtChangeEventDatasetVersionDeleted e ->
                datasetVersionService.deleteDatasetVersionAfterDeletingItems(e.getDatasetVersionId());
            case VirtChangeEventDeleteDatasetVersionError e ->
                datasetVersionService.changeStateDatasetVersion(e.getDatasetVersionId(), DatasetState.DELETE_ERROR);
            case VirtChangeEventUpdateDatasetVersionState e ->
                datasetVersionService.updateState(datasetVersionMapper.toModel(e.getDatasetVersionDto()));
            case VirtChangeEventUpdateDatasetVersionStateAndMessageCreated e -> {
                datasetVersionService.updateState(datasetVersionMapper.toModel(e.getDatasetVersionDto()));
                datasetVersionMessageService.save(e.getImportsMessage());
            }
            default -> log.infof("Event received but not used %s", event);
        }
    }
}