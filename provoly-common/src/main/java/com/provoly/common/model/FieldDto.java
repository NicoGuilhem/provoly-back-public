package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class FieldDto {

    private static final String CRS_PREFIX = "EPSG:";
    public UUID id;
    public String name;
    public String type;
    public String slug;
    public String crs; // CRS in format as "EPSG:2154"

    public FieldDto() {
    }

    public FieldDto(UUID id, String name, String type, String slug, String crs) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.slug = slug;
        this.crs = crs;
    }

    public Type getType() {
        return Type.from(type);
    }

    @Override
    public String toString() {
        return "FieldDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", slug='" + slug + '\'' +
                '}';
    }

    public int checkAndExtractSRID() {
        if (!getType().isGeo()) {
            if (crs != null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "CRS should be blank for non geo field");
            }
            return -1;
        }

        if (getType().isGeo()) {
            if (crs == null || crs.isBlank()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "CRS required for geo field");
            }
            if (!crs.startsWith(CRS_PREFIX)) {
                throw new BusinessException(ErrorCode.TECHNICAL, "Field: " + this + " have an incompatible CRS: " + crs);
            }
        }
        try {
            var sridString = crs.substring(CRS_PREFIX.length());
            return Integer.parseInt(sridString);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Field: " + this + " have an incompatible CRS");
        }
    }
}
