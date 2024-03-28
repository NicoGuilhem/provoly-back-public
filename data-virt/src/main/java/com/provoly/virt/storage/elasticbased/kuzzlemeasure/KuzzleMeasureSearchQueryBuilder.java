package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.SearchQueryBuilder;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleMeasureSearchQueryBuilder extends SearchQueryBuilder {

    public KuzzleMeasureSearchQueryBuilder(Logger log, @RestClient MetadataRefService metadataService, StorageSupport support,
            KuzzleMeasureLayout storageLayout) {
        super(log, metadataService, support, storageLayout);
    }
}
