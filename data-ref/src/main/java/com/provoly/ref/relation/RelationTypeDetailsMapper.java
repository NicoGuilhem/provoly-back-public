package com.provoly.ref.relation;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.relation.RelationTypeDetailsDto;
import com.provoly.ref.link.LinkService;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class RelationTypeDetailsMapper {

    @Inject
    LinkService linkService;

    @Inject
    RelationTypeStatsService relationTypeStatsService;

    @AfterMapping
    void getRelationTypeDetailsDto(RelationType relationType, @MappingTarget RelationTypeDetailsDto result) {
        result.nbLink = linkService.countLinkByRelationType(relationType);
        result.nbRelation = relationType.getRelationTypeStats().getNbRelation();
        result.deletable = result.nbLink == 0 && result.nbRelation == 0;
        result.modificationDate = relationType.getModificationDate();
    }

    @AfterMapping
    void updateDateModification(@MappingTarget RelationType relationType) {
        relationType.setModificationDate(Instant.now());
        RelationTypeStats rts = relationTypeStatsService.findById(relationType.getId());
        try {
            if (rts == null) {
                Random random = SecureRandom.getInstanceStrong();
                rts = new RelationTypeStats(relationType.getId(), random.nextInt(10));
            }
            relationType.setRelationTypeStats(rts);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Failed to update RelationType %s.".formatted(relationType.getId()), e);
        }
    }

}
