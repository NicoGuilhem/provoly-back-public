package com.provoly.virt.storage;

import java.util.Collection;

import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.virt.entity.Relation;

public interface StorageRelationWriterService {
    void save(Collection<Relation> relations);

    void updateAggregate(Collection<RelationsAggregateDto> relationsAggregates);

    void delete(Relation relation);

}
