package com.provoly.virt.storage.elasticbased;

import java.time.Instant;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.field.FieldGeoDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.Item;

@ApplicationScoped
public class ElasticSupport {

    public static final GeoFormat elasticGeoFormat = GeoFormat.WKT;
    public static final int ES_HARD_LIMIT = 10000;

    // Only used by elastic based storage, should be moved in elastic package
    public void extractAttributeValue(AttributeSimpleValue attributeValue, Object attribute) {
        var value = switch (attributeValue.getFieldType().getTypeCategory()) {
            case GEO -> new GeoHolder(attribute, elasticGeoFormat, ((FieldGeoDto) attributeValue.getField()).getCrs());
            case DATE -> toIsoDate(attribute.toString());
            default -> attribute;
        };
        attributeValue.setValue(value);
    }

    public void validateSearchLimit(MonoClassRequestDto request) throws BusinessException {
        if (request.getLimit() > ES_HARD_LIMIT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Request limit can't be greater than 10000 for an Elastic based Storage");
        }
    }

    public void setAttributeValue(OClassDetailsDto oClass, Item item, String key, Object value) {
        var attributeDef = findAttributeByName(oClass, key);
        attributeDef.ifPresent(att -> {
            var attributeSimple = item.getAttributeSimple(att.getName());
            extractAttributeValue(attributeSimple, value);
        });
    }

    public Optional<AttributeDefDetailsDto> findAttributeByName(OClassDetailsDto oClass, String attributeName) {
        return oClass.getAttributes().stream()
                .filter(a -> a.getName().equals(attributeName))
                .findAny();
    }

    public Instant toIsoDate(String date) {
        try {
            long timestamp = Long.parseLong(date);
            return Instant.ofEpochMilli(timestamp);
        } catch (NumberFormatException nfe) {
            return Instant.parse(date);
        }
    }
}
