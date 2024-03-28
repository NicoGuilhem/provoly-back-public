package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.Storage;
import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.virt.entity.Relation;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageRelationWriterAdapters implements StorageRelationWriterService {

    private final Logger log;
    private final Instance<StorageRelationWriterService> searchStorages;

    public StorageRelationWriterAdapters(Logger log, @Any Instance<StorageRelationWriterService> storages) {
        this.log = log;
        this.searchStorages = storages;
    }

    @Override
    public void save(Collection<Relation> relations) {
        log.debugf("Start a saving relation ");
        getService(searchStorages, Storage.ELASTIC).save(relations);
    }

    @Override
    public void updateAggregate(Collection<RelationsAggregateDto> relationsAggregates) {
        log.debugf("Updating relation");
        getService(searchStorages, Storage.ELASTIC).updateAggregate(relationsAggregates);
    }

    @Override
    public void delete(Relation relation) {
        log.debugf("Deleting relation");
        getService(searchStorages, Storage.ELASTIC).delete(relation);
    }

}
