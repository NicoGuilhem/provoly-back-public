package com.provoly.virt.entity;

import java.util.Objects;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import io.quarkus.runtime.util.HashUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ItemId {

    private static final String SEPARATOR = "@";
    private static final String UNDISCLOSED_MARK = "~_~";
    private final UUID datasetVersionId;
    private final String id;

    public static ItemId buildUndisclosed(ItemId id) {
        return new ItemId(id.getDatasetVersionId(), UNDISCLOSED_MARK + HashUtil.sha1(id.getId()));
    }

    @JsonCreator
    public ItemId(String composedId) {
        var parts = composedId.split(SEPARATOR, 2);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Id have incorrect format :" + composedId);
        }
        this.datasetVersionId = UUID.fromString(parts[0]);
        this.id = parts[1];
        if (this.id.startsWith(UNDISCLOSED_MARK)) {
            throw new UnsupportedOperationException("We should not build item from undisclosed id");
        }
    }

    public ItemId(UUID datasetVersionId, String id) {
        this.datasetVersionId = datasetVersionId;
        this.id = id;
    }

    @JsonValue
    public String getAsString() {
        return datasetVersionId + SEPARATOR + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ItemId itemId = (ItemId) o;
        return datasetVersionId.equals(itemId.datasetVersionId) && id.equals(itemId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasetVersionId, id);
    }

    @Override
    public String toString() {
        return "ItemId{" + getAsString() + '}';
    }

    public String getId() {
        return id;
    }

    public UUID getDatasetVersionId() {
        return datasetVersionId;
    }
}
