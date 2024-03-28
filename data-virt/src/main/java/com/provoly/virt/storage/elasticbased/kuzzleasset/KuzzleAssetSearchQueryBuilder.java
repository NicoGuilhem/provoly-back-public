package com.provoly.virt.storage.elasticbased.kuzzleasset;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.SearchQueryBuilder;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleAssetSearchQueryBuilder extends SearchQueryBuilder {

    public KuzzleAssetSearchQueryBuilder(Logger log, @RestClient MetadataRefService metadataService, StorageSupport support,
            KuzzleAssetLayout storageLayout) {
        super(log, metadataService, support, storageLayout);
    }
}
