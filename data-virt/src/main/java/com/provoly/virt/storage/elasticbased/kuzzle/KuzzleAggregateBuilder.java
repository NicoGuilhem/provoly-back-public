package com.provoly.virt.storage.elasticbased.kuzzle;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.AggregateQueryBuilder;

import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleAggregateBuilder extends AggregateQueryBuilder {

    public KuzzleAggregateBuilder(Logger log, StorageSupport storageSupport, KuzzleLayout storageLayout) {
        super(storageSupport, storageLayout, log);
    }

}
