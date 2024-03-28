package com.provoly.transfo.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.search.Operator;
import com.provoly.common.transfo.Filter;
import com.provoly.common.transfo.NodeDto;
import com.provoly.common.transfo.TransfoDto;

import io.quarkus.test.junit.QuarkusTest;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@QuarkusTest
public class TransfoMappingTest {

    @Inject
    Logger log;

    @Inject
    ObjectMapper mapper;

    //    private final ObjectMapper mapper = JsonMapper.builder()
    //            .build().findAndRegisterModules(); // Needed to register ParametersModule

    /** Only to test if parameter name are effectively activate during compilation */
    @Test
    public void serdeCtor() throws JsonProcessingException {
        var simpleDto = new SimpleDto("expected");
        String json = mapper.writeValueAsString(simpleDto);
        log.infof("Serialized value : %s", json);
        var result = mapper.readValue(json, SimpleDto.class);
    }

    @Test
    public void serdeNodeTaskDto() throws JsonProcessingException {
        var filter = new Filter("attrName", Operator.GREATER_THAN, 3);
        var node = new NodeDto(filter);
        String json = mapper.writeValueAsString(node);
        log.infof("Serialized value : %s", json);
        var task = mapper.readValue(json, NodeDto.class);
        assertThat(task).isOfAnyClassIn(NodeDto.class);
        assertThat(task.getSpec()).isOfAnyClassIn(Filter.class);
    }

    @Test
    public void serdeTransfoDto() throws JsonProcessingException {
        var filter = new Filter("attrName", Operator.GREATER_THAN, 3);
        var node = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(node), "Titre");
        String json = mapper.writeValueAsString(transfo);
        log.infof("Serialized value : %s", json);

        var result = mapper.readValue(json, TransfoDto.class);
        assertThat(result.getNodes()).hasSize(1)
                .first()
                .extracting(v -> v.getSpec().getClass())
                .isEqualTo(Filter.class);

    }

}
