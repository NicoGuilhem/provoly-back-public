package com.provoly.transfo.engine;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;

import com.provoly.common.transfo.NodeSpec;

import io.quarkus.jackson.ObjectMapperCustomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

// Inspired from https://quarkus.io/guides/rest-json
// Needed to allow polymorphic deserialization of tasks
public class ProvolyObjectMapper {

    @Singleton
    @Produces
    ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {

        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(NodeSpec.class)
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .polymorphicTypeValidator(ptv)
                .build();

        // Apply all ObjectMapperCustomizer beans (incl. Quarkus)
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(mapper);
        }

        return mapper;

    }

}
