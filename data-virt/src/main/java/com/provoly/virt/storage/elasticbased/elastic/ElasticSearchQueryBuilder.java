package com.provoly.virt.storage.elasticbased.elastic;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.SearchQueryBuilder;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
class ElasticSearchQueryBuilder extends SearchQueryBuilder {

    public ElasticSearchQueryBuilder(Logger log, @RestClient MetadataRefService metadataService, StorageSupport support,
            ElasticLayout storageLayout) {
        super(log, metadataService, support, storageLayout);
    }
}
