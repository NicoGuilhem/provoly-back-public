package com.provoly.virt.storage.elasticbased.elastic;

import java.io.IOException;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.LinkService;
import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.RelationAttributes;
import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.Relation;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageRelationWriterService;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;

@ApplicationScoped
@StorageQualifier(Storage.ELASTIC)
class ElasticRelationWriterService implements StorageRelationWriterService {
    private DataVirtProperties properties;
    Logger log;

    ElasticsearchClient elasticClient;

    ElasticRelationService relationService;

    @RestClient
    LinkService linkService;

    ElasticLayout elsaticSupport;

    public ElasticRelationWriterService(Logger log,
            ElasticsearchClient elasticClient,
            @StorageQualifier(Storage.ELASTIC) ElasticRelationService relationService,
            @RestClient LinkService linkService,
            ElasticLayout elasticSupport,
            DataVirtProperties dataVirtProperties) {
        this.log = log;
        this.elasticClient = elasticClient;
        this.relationService = relationService;
        this.linkService = linkService;
        this.elsaticSupport = elasticSupport;
        this.properties = dataVirtProperties;
    }

    public void save(Collection<Relation> relations) {
        if (relations.isEmpty())
            return;

        try {
            var bulkRequest = new BulkRequest.Builder();
            for (var relation : relations) {
                // Object relation is build at least to control Ids
                bulkRequest.operations(o -> o
                        .index(i -> i
                                .index(properties.relationIndexName())
                                .id(buildId(relation))
                                .document(buildElasticRelation(relation))));
            }
            elsaticSupport.prepareRequest(bulkRequest);
            log.infof("Adding %s relation(s)", relations.size());
            var response = elasticClient.bulk(bulkRequest.build());
            if (response.errors()) {
                throw new BusinessException(ErrorCode.TECHNICAL, "Add relations failed : " + response.items());
            }
            log.debugf("  Took %d / ingest took %d", response.took(), response.ingestTook());

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error during insert relations into elastic", e);
        }
    }

    public void updateAggregate(Collection<RelationsAggregateDto> relationsAggregates) {
        log.debugf("Starting a new updateAggregate of %d aggregates", relationsAggregates.size());
        int bulkRelationsCouht = 0;
        var relationsAggregatesBulk = new ArrayList<RelationsAggregateDto>();

        for (RelationsAggregateDto relationsAggregate : relationsAggregates) {
            int relationCount = relationsAggregate.source.size() * relationsAggregate.dest.size();
            // TODO : Check max relation count
            bulkRelationsCouht += relationCount;
            relationsAggregatesBulk.add(relationsAggregate);
            if (bulkRelationsCouht > 1000) {
                updateAggregateImpl(relationsAggregatesBulk);
                relationsAggregatesBulk.clear();
                bulkRelationsCouht = 0;
            }
        }
        updateAggregateImpl(relationsAggregatesBulk);
        log.debugf("UpdateAggregate done");
    }

    private void updateAggregateImpl(Collection<RelationsAggregateDto> relationsAggregates) {
        long start = System.currentTimeMillis();
        var oldRelations = relationService.getRelationsByAggregates(relationsAggregates);
        var newRelations = generateRelationsFor(relationsAggregates);

        var relationToCreate = new HashSet<>(newRelations);
        relationToCreate.removeAll(oldRelations); // relationToCreate contains all relations that not exists previously
        oldRelations.removeAll(newRelations); // oldRelation contains all relations that disappeared

        log.debugf("Updating %d aggregates relations. %d relations with %d delete and %d creations",
                relationsAggregates.size(),
                newRelations.size(),
                oldRelations.size(),
                relationToCreate.size());
        delete(oldRelations);
        save(relationToCreate);
        log.debugf("Updated in %dms", System.currentTimeMillis() - start);
    }

    public void delete(Relation relation) {
        try {
            String id = buildId(relation);
            var deleteRequest = new DeleteRequest.Builder()
                    .index(properties.relationIndexName())
                    .id(id);
            elsaticSupport.prepareRequest(deleteRequest);
            var response = elasticClient.delete(deleteRequest.build());
            if (response.result() != Result.Deleted) {
                throw new BusinessException(ErrorCode.TECHNICAL, "Error during delete of relation with id " + id);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error during removing relations from elastic", e);
        }
    }

    public void delete(Collection<Relation> relation) {
        try {
            var ids = relation.stream().map(this::buildId).toList();

            var response = elasticClient.deleteByQuery(d -> d
                    .index(properties.relationIndexName())
                    .query(q -> q
                            .ids(i -> i.values(ids))));

            if (response.deleted() != relation.size()) {
                throw new BusinessException(ErrorCode.TECHNICAL,
                        "Error during delete of relation with id " + ids + "=>" + response);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error during removing relations from elastic", e);
        }
    }

    private Collection<Relation> generateRelationsFor(Collection<RelationsAggregateDto> relationsAggregates) {
        var relations = new HashSet<Relation>();

        for (RelationsAggregateDto relationsAggregate : relationsAggregates) {
            var link = linkService.getById(relationsAggregate.link);
            for (String source : relationsAggregate.source) {
                for (String dest : relationsAggregate.dest) {
                    var relation = new Relation(link.getRelationType().slug, new ItemId(source), new ItemId(dest),
                            relationsAggregate.aggregateId);
                    relations.add(relation);
                }
            }
        }
        return relations;
    }

    private Map<String, ?> buildElasticRelation(Relation relation) {
        var relationMap = new HashMap<String, Object>();
        relationMap.put(RelationAttributes.SOURCE, relation.getSource().getAsString());
        relationMap.put(RelationAttributes.DESTINATION, relation.getDestination().getAsString());
        relationMap.put(RelationAttributes.TYPE, relation.getRelationType());
        relationMap.put(RelationAttributes.AGGREGATE_ID, relation.getAggregateId());
        return relationMap;
    }

    private String buildId(Relation relation) {
        return relation.getSource().getAsString() + "_" + relation.getDestination().getAsString() + "_"
                + relation.getRelationType();
    }

}
