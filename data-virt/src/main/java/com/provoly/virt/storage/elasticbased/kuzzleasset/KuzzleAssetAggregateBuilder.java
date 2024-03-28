package com.provoly.virt.storage.elasticbased.kuzzleasset;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.AggregateQueryBuilder;

import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleAssetAggregateBuilder extends AggregateQueryBuilder {

    public KuzzleAssetAggregateBuilder(Logger log, StorageSupport storageSupport, KuzzleAssetLayout storageLayout) {
        super(storageSupport, storageLayout, log);
    }

}
