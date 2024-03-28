package com.provoly.ref.datasetversion;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntityId;

@Entity
public class DatasetVersionMessage extends EntityId {
    @Enumerated(EnumType.STRING)
    private MessageLevel level;

    @Enumerated(EnumType.STRING)
    private ExtractMessageCode extractMessageCode;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String name;

    private String recordId;

    private UUID datasetVersionId;

    public DatasetVersionMessage() {
    }

    public DatasetVersionMessage(UUID id) {
        super(id);
    }

    public DatasetVersionMessage(UUID id, MessageLevel level, UUID datasetVersionId, String recordId) {
        this.id = id;
        this.level = level;
        this.datasetVersionId = datasetVersionId;
        this.recordId = recordId;
    }

    public ExtractMessageCode getExtractMessageCode() {
        return extractMessageCode;
    }

    public void setExtractMessageCode(ExtractMessageCode extractMessageCode) {
        this.extractMessageCode = extractMessageCode;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String lineId) {
        this.recordId = lineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DatasetVersionError{" +
                "extractMessageCode=" + extractMessageCode +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", recordId='" + recordId + '\'' +
                ", id=" + id +
                '}';
    }

    public UUID getDatasetVersionId() {
        return datasetVersionId;
    }

    public void setDatasetVersionId(UUID datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    public MessageLevel getLevel() {
        return level;
    }

    public void setLevel(MessageLevel level) {
        this.level = level;
    }

}
