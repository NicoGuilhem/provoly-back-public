package com.provoly.test;

import java.util.Map;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.kafka.KafkaCompanionResource;

/*
    Override KafkaCompanionResource to avoid the automatic generation of
    the strimzi devservice to use companion and to take into account
    the container configured for testing
 */
public class ProvolyKafkaCompanionResource extends KafkaCompanionResource {

    private DevServicesContext context;
    private Map<String, String> initArgs;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

    @Override
    public Map<String, String> start() {
        super.setIntegrationTestContext(context);
        super.init(initArgs);
        return super.start();
    }
}
