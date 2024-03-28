package com.provoly.virt.relation;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.relation.RelationDto;
import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.Relation;
import com.provoly.virt.storage.StorageRelationWriterAdapters;

@ApplicationScoped
public class RelationService {

    @Inject
    StorageRelationWriterAdapters storageRelationAdapters;

    public void saveRelations(Collection<RelationDto> relationsDto) {
        var relations = relationsDto.stream().map(this::buildRelation).collect(Collectors.toList());
        storageRelationAdapters.save(relations);
        // TODO : Inject to relation topic, issue #431
    }

    public void updateAggregate(Collection<RelationsAggregateDto> relationsAggregate) {
        storageRelationAdapters.updateAggregate(relationsAggregate);
        // TODO : Inject to relation topic, issue #431
    }

    public void delete(RelationDto relationDto) {
        var relation = buildRelation(relationDto);
        storageRelationAdapters.delete(relation);
        // TODO : Inject to relation topic, issue #431
    }

    // TODO : Use mapper
    private Relation buildRelation(RelationDto relationDto) {
        return new Relation(relationDto.getRelationType(), new ItemId(relationDto.getSource()),
                new ItemId(relationDto.getDestination()));
    }
}
