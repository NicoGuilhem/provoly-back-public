package com.provoly.virt.storage.elasticbased.elastic;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import com.provoly.common.Storage;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.storage.StorageQualifier;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@ApplicationScoped
class ElasticRelationIndexBuilderProducer {

    private Optional<ElasticsearchClient> elasticClient;

    private Instance<ElasticModelService> elasticModelService;

    public ElasticRelationIndexBuilderProducer(Instance<ElasticsearchClient> elasticClient,
            @StorageQualifier(Storage.ELASTIC) Instance<ElasticModelService> elasticModelService) {
        this.elasticClient = Optional.ofNullable(elasticClient.get());
        this.elasticModelService = elasticModelService;
    }

    @Produces
    @StorageQualifier(Storage.ELASTIC)
    public ElasticRelationIndexBuilder get(DataVirtProperties datavirtProperties) {
        //Only check elasticClient presence. If it's non null, then elasticModelServices won't be null either
        return elasticClient.map(client -> new ElasticRelationIndexBuilder(Logger.getLogger(ElasticRelationIndexBuilder.class),
                elasticModelService.get(),
                client,
                datavirtProperties))
                .orElse(null);
    }
}
