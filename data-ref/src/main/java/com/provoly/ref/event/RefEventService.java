package com.provoly.ref.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.model.FieldDto;
import com.provoly.common.ref.*;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.OClass;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RefEventService {

    private Logger log;

    private ModelMapper mapper;
    private DatasetVersionMapper datasetVersionMapper;

    @Inject
    @Channel(RefChangeEvent.TOPIC_NAME)
    Emitter<RefChangeEvent> eventEmitter;

    public RefEventService(Logger log, ModelMapper mapper, DatasetVersionMapper datasetVersionMapper) {
        this.log = log;
        this.mapper = mapper;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void fieldAdded(FieldDto field) {
        log.infof("Sending event field Added %s", field);
        var refEvent = new RefChangeEventFieldAdded(field);
        eventEmitter.send(refEvent);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void classCreated(OClass oClass) {
        log.infof("Sending event class created %s", oClass);
        var detailsDto = mapper.toDetailsDto(oClass);
        var refEvent = new RefChangeEventClassCreated(detailsDto);
        eventEmitter.send(refEvent);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void classUpdated(OClass oClass) {
        log.infof("Sending event class updated %s", oClass);
        var detailsDto = mapper.toDetailsDto(oClass);
        var refEvent = new RefChangeEventClassUpdated(detailsDto);
        eventEmitter.send(refEvent);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void classDeleted(OClass oClass) {
        log.infof("Sending event class deleted %s", oClass);
        var detailsDto = mapper.toDetailsDto(oClass);
        var refEvent = new RefChangeEventClassDeleted(detailsDto);
        eventEmitter.send(refEvent);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void datasetActivated(DatasetVersion datasetVersion) {
        log.infof("Sending event dataset version Activated %s", datasetVersion);
        var refEvent = new RefChangeEventDatasetVersionActivated(datasetVersion.getDataset().getId(),
                datasetVersion.getId());
        eventEmitter.send(refEvent);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void datasetVersionDeleted(DatasetVersion datasetVersion) {
        log.infof("Sending event dataset version deleted %s", datasetVersion.getId());
        var refEvent = new RefChangeEventDatasetVersionDeleted(datasetVersionMapper.toDto(datasetVersion),
                mapper.toDetailsDto(datasetVersion.getDataset().getoClass()));
        eventEmitter.send(refEvent);
    }
}