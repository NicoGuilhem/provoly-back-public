package com.provoly.common.ref;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RefChangeEventDatasetVersionActivated.class, name = "DATASET_VERSION_ACTIVATED"),
        @JsonSubTypes.Type(value = RefChangeEventDatasetVersionDeleted.class, name = "DATASET_VERSION_DELETED"),
        @JsonSubTypes.Type(value = RefChangeEventDatasetDeleted.class, name = "DATASET_DELETED"),
        @JsonSubTypes.Type(value = RefChangeEventClassCreated.class, name = "CLASS_CREATED"),
        @JsonSubTypes.Type(value = RefChangeEventClassReady.class, name = "CLASS_READY"),
        @JsonSubTypes.Type(value = RefChangeEventClassUpdated.class, name = "CLASS_UPDATED"),
        @JsonSubTypes.Type(value = RefChangeEventClassDeleted.class, name = "CLASS_DELETED"),
        @JsonSubTypes.Type(value = RefChangeEventFieldAdded.class, name = "FIELD_ADDED"),
})
public abstract class RefChangeEvent {

    // If change topic-name don't forget to change application.yaml
    public static final String TOPIC_NAME = "ref-event";

    public enum Type {
        DATASET_VERSION_ACTIVATED,
        DATASET_VERSION_DELETED,
        DATASET_DELETED,
        CLASS_CREATED,
        CLASS_UPDATED,
        CLASS_DELETED,
        CLASS_READY,
        FIELD_ADDED
    }

    private final Type type;

    RefChangeEvent(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RefChangeEvent{" +
                "type=" + type +
                '}';
    }

    public Type getType() {
        return type;
    }
}
