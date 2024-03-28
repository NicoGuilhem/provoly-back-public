package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.AggregateQueryBuilder;

import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleMeasureAggregateBuilder extends AggregateQueryBuilder {

    public KuzzleMeasureAggregateBuilder(Logger log, StorageSupport storageSupport, KuzzleMeasureLayout storageLayout) {
        super(storageSupport, storageLayout, log);
    }

}
