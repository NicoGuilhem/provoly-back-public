package com.provoly.common.virt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({

        @JsonSubTypes.Type(value = VirtChangeEventImportMessageCreated.class, name = "IMPORT_MESSAGE"),
        @JsonSubTypes.Type(value = VirtChangeEventDatasetVersionDeleted.class, name = "DELETED_DATASET_VERSION"),
        @JsonSubTypes.Type(value = VirtChangeEventDeleteDatasetVersionError.class, name = "DELETE_DATASET_VERSION_ERROR"),
        @JsonSubTypes.Type(value = VirtChangeEventUpdateDatasetVersionState.class, name = "UPDATE_DATASET_VERSION_STATE"),
        @JsonSubTypes.Type(value = VirtChangeEventUpdateDatasetVersionState.class, name = "UPDATE_DATASET_VERSION_STATE_AND_IMPORT_MESSAGE")
})
public abstract class VirtChangeEvent {

    // If topic-name changes, don't forget to change application.yaml accordingly
    public static final String TOPIC_NAME = "virt-event";

    public enum Type {
        IMPORT_MESSAGE,
        DELETED_DATASET_VERSION,
        DELETE_DATASET_VERSION_ERROR,
        UPDATE_DATASET_VERSION_STATE,
        UPDATE_DATASET_VERSION_STATE_AND_IMPORT_MESSAGE
    }

    private final Type type;

    VirtChangeEvent(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirtChangeEvent{" +
                "type=" + type +
                '}';
    }

    public Type getType() {
        return type;
    }
}