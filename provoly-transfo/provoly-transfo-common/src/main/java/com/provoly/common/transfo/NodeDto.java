package com.provoly.common.transfo;

import java.util.Objects;
import java.util.UUID;

import com.provoly.common.Default;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Schema
public class NodeDto {
    private final UUID id;
    private final String type;
    private final String title;
    private final GuiDto gui;

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
    private final NodeSpec spec;

    @Default
    @JsonCreator
    public NodeDto(UUID id, String type, String title, GuiDto gui, NodeSpec spec) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.gui = gui;
        this.spec = spec;
    }

    public NodeDto(NodeSpec nodeSpec) {
        this(UUID.randomUUID(), nodeSpec);
    }

    public NodeDto(UUID id, NodeSpec nodeSpec) {
        this.id = id;
        this.type = nodeSpec.getClass().getName();
        this.spec = nodeSpec;
        title = null;
        gui = null;
    }

    public TransfoNodeStatus validate(TransfoNodeStatus previousNode) {
        if (previousNode == null && spec.needInput()) {
            return new TransfoNodeStatus(id, new TransfoNodeErrorNoInput());
        }
        return spec.validate(id, previousNode.getOutModel());
    }

    public <T> boolean isType(Class<T> specClass) {
        return specClass.isInstance(spec);
    }

    public <T extends NodeSpec> T specAs(Class<T> specClass) {
        return specClass.cast(spec);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeDto nodeDto = (NodeDto) o;
        return id.equals(nodeDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NodeDto{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", spec=" + spec +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public GuiDto getGui() {
        return gui;
    }

    public NodeSpec getSpec() {
        return spec;
    }

}
