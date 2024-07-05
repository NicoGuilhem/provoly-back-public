package com.provoly.common.model.field;

import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class FieldGeoDto extends FieldDto {
    private static final String CRS_PREFIX = "EPSG:";
    private String crs;

    public FieldGeoDto(UUID id, String name, String type, String slug, String crs) {
        super(id, name, type, slug);
        this.crs = crs;
    }

    public String getCrs() {
        return crs;
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

    @Override
    public void checkField() throws BusinessException {
        this.checkAndExtractSRID();
    }
}
