package com.provoly.transfo;

import java.util.UUID;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import com.provoly.EntityId;
import com.provoly.common.Default;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class Node extends EntityId {
    private String type;
    private String title;
    @Embedded
    private Gui gui;
    @Type(JsonNodeUserType.class)
    private JsonNode spec;

    @Default
    public Node(UUID id) {
        super(id);
    }

    public Node() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Gui getGui() {
        return gui;
    }

    public void setGui(Gui gui) {
        this.gui = gui;
    }

    @Type(JsonNodeUserType.class)
    public JsonNode getSpec() {
        return spec;
    }

    public void setSpec(JsonNode spec) {
        this.spec = spec;
    }
}
