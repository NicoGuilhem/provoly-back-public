package com.provoly.transfo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.transfo.NodeSpec;

import org.mapstruct.Named;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class NodeSpecMapper {

    @Inject
    ObjectMapper mapper;

    JsonNode nodeSpecToJsonNode(NodeSpec spec) {
        return mapper.valueToTree(spec);
    }

    @Named("jsonNodetoNodeSpec")
    NodeSpec jsonNodetoNodeSpec(Node node) {
        try {
            return mapper.treeToValue(node.getSpec(), Class.forName(node.getType()).asSubclass(NodeSpec.class));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
