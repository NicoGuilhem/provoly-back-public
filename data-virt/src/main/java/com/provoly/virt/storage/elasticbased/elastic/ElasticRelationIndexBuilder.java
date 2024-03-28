package com.provoly.virt.storage.elasticbased.elastic;

import java.io.IOException;
import java.text.MessageFormat;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.RelationAttributes;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.storage.StorageInitEventListener;
import com.provoly.virt.storage.StorageQualifier;

import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;

@StorageQualifier(Storage.ELASTIC)
class ElasticRelationIndexBuilder implements StorageInitEventListener {

    private DataVirtProperties properties;

    private Logger log;

    private ElasticModelService elasticModelService;

    private ElasticsearchClient elasticClient;

    public ElasticRelationIndexBuilder(Logger log,
            ElasticModelService elasticModelService,
            ElasticsearchClient elasticClient,
            DataVirtProperties dataVirtProperties) {
        this.log = log;
        this.elasticModelService = elasticModelService;
        this.elasticClient = elasticClient;
        this.properties = dataVirtProperties;
    }

    /**
     * Create the index relation if it does not exist
     */
    @Override
    public void onInitEvent(StartupEvent ev) {
        try {
            if (elasticModelService.indexExists(properties.relationIndexName())) {
                log.infof("Index %s already exist", properties.relationIndexName());
            } else {
                log.infof("Creating index %s", properties.relationIndexName());
                var response = elasticClient.indices().create(c -> c
                        .index(properties.relationIndexName())
                        .mappings(buildMapping()));

                if (!response.acknowledged()) {
                    throw new IllegalStateException(
                            MessageFormat.format("Unable to create {0} index", properties.relationIndexName()));
                }
            }

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    MessageFormat.format("Error creating index {0}", properties.relationIndexName()), e);
        }
    }

    private TypeMapping buildMapping() {

        return TypeMapping.of(t -> t
                .properties(RelationAttributes.TYPE, p -> p.keyword(k -> k))
                .properties(RelationAttributes.SOURCE, p -> p.keyword(k -> k))
                .properties(RelationAttributes.DESTINATION, p -> p.keyword(k -> k))
                .properties(RelationAttributes.AGGREGATE_ID, p -> p.keyword(k -> k)));

    }
}
