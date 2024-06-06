package com.provoly.virt.storage;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.entity.SearchAfterContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class StorageSupport {

    private ObjectMapper mapper;

    public static final String LAT = "lat";
    public static final String LON = "lon";
    public static final String BOTTOM_RIGHT = "bottom_right";
    public static final String TOP_LEFT = "top_left";

    public StorageSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SearchAfterContext getSearchAfterContext(String searchAfter) throws JsonProcessingException {
        String searchAfterContextDecoded = new String(Base64.getDecoder().decode(searchAfter));
        return mapper.readValue(searchAfterContextDecoded, SearchAfterContext.class);
    }

    public AttributeDefDetailsDto getAttributeDetail(OClassDetailsDto classDto, UUID attr) {
        return attr == null ? null
                : getAttributeById(classDto, attr);
    }

    public void checkFieldTypeIsNumeric(AttributeDefDetailsDto attribute) {
        checkPresence(attribute);

        if (!attribute.getField().getType().isNumeric()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "It's not possible to aggregate on the non-numeric attribute %s which has type %s".formatted(
                            attribute.getName(),
                            attribute.getField().type));
        }
    }

    public void checkFieldTypeIsDate(AttributeDefDetailsDto attribute) {
        checkPresence(attribute);

        if (!attribute.getField().getType().equals(Type.INSTANT)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Aggregating on date is unavailable for attribute %s that is not a date.".formatted(attribute.getName()));
        }
    }

    public void checkFieldTypeIsGeo(AttributeDefDetailsDto attribute) {
        checkPresence(attribute);

        if (!attribute.getField().getType().isGeo()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Aggregation on the non-geo attribute %s which has type %s is not possible.".formatted(attribute.getName(),
                            attribute.getField().type));
        }
    }

    private void checkPresence(AttributeDefDetailsDto value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Attribute required for this aggregation");
        }
    }

    public AttributeDefDetailsDto getAttributeById(OClassDetailsDto oClass, UUID attributeId) {
        return oClass.getAttributeById(attributeId).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                "No attribute " + attributeId + " belongs to class " + oClass.getName()));
    }

    public List<AttributeDefDetailsDto> getAttributesByIds(OClassDetailsDto oClass, List<UUID> attributesId) {
        return attributesId.stream().map(id -> getAttributeById(oClass, id)).toList();
    }

}
