package com.provoly.virt.imports;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.ExtractedMessage;
import com.provoly.common.imports.FileImportDto;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.imports.model.ConversionResult;
import com.provoly.virt.imports.model.ItemRecord;

import org.jboss.logging.Logger;

@ApplicationScoped
public class RecordConvertor {

    private Logger log;

    public RecordConvertor(Logger log) {
        this.log = log;
    }

    public List<ExtractedMessage> validateHeaders(List<String> headers, List<String> oClassAttributes) {

        List<ExtractedMessage> headerMessages = new ArrayList<>();
        HashSet<String> headersHashSet = new HashSet<>(headers);
        HashSet<String> oClassAttributHashSet = new HashSet<>(oClassAttributes);

        log.debug("Check if at least one oclass attribute is present");
        var attributeFiltered = headers
                .stream()
                .filter(oClassAttributHashSet::contains)
                .toList();
        if (attributeFiltered.isEmpty()) {
            headerMessages.add(new ExtractedMessage(MessageLevel.ERROR,
                    ExtractMessageCode.NO_ATTRIBUTES));
        }
        log.debug("Check whether any additional attributes are presents");
        headerMessages.addAll(headersHashSet
                .stream()
                .filter(attr -> !oClassAttributHashSet.contains(attr))
                .map(unknownAttr -> new ExtractedMessage(MessageLevel.WARNING,
                        ExtractMessageCode.UNRECOGNIZED,
                        new FileImportDto.ParamsTypeError(unknownAttr)))
                .toList());

        return headerMessages;
    }

    public ConversionResult convert(ItemRecord itemRecord, OClassDetailsDto oClassDetailsDto) {
        return this.convert(itemRecord, oClassDetailsDto, false);
    }

    public ConversionResult convert(ItemRecord itemRecord, OClassDetailsDto oClassDetailsDto, boolean normalizeGeo) {
        if (itemRecord.values().entrySet().stream().noneMatch(entry -> entry.getValue() != null)) {
            log.error("No values in extracted record");
            return new ConversionResult(null,
                    List.of(new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.NO_VALUES)));
        }
        Map<String, Object> values = new HashMap<>();
        List<ExtractedMessage> errors = new ArrayList<>();
        oClassDetailsDto.getAttributes().stream()
                .filter(attribute -> itemRecord.values().get(attribute.technicalName) != null)
                .forEach(attribute -> assignOrGetError(attribute, itemRecord, values, errors, normalizeGeo));

        return new ConversionResult(new ItemRecord(itemRecord.recordId(), values), errors); // retourne soit l'item soit la liste des erreurs
    }

    private Object assignTo(Object value, Type type, boolean normalizeGeo, String crs) {
        return switch (value) {
            case Integer i -> assignTo(i, type);
            case Long l -> assignTo(l, type);
            case String s -> assignTo(s, type, normalizeGeo, crs);
            case Double d -> assignTo(d, type);
            case Date d -> assignTo(d, type);
            case Instant instant -> assignTo(instant, type);
            case GeoHolder geo -> assignTo(geo, type, normalizeGeo);
            default -> throw new BusinessException(com.provoly.common.error.ErrorCode.BAD_REQUEST,
                    "Cannot assign value %s, type not supported".formatted(value));
        };
    }

    private Object assignTo(Integer value, Type type) {
        return switch (type) {
            case LONG -> Long.valueOf(value);
            case DECIMAL -> Double.valueOf(value);
            case STRING, RAW, INTEGER, KEYWORD -> value;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, String.valueOf(value));
        };
    }

    private Object assignTo(Long value, Type type) {
        return switch (type) {
            case INTEGER -> Math.toIntExact(value);
            case DECIMAL -> Double.valueOf(value);
            case LONG, STRING, RAW, KEYWORD -> value;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, String.valueOf(value));
        };

    }

    private Object assignTo(String value, Type type, boolean normalizeGeo, String crs) {
        if (value.isBlank()) {
            return null;
        }
        return switch (type) {
            case STRING, KEYWORD, RAW -> value;
            case INTEGER -> Integer.valueOf(value);
            case LONG -> Long.valueOf(value);
            case DECIMAL -> Double.valueOf(value);
            case INSTANT -> {
                DateTimeFormatter isoFormat = DateTimeFormatter.ISO_DATE_TIME;
                yield ZonedDateTime.parse(value, isoFormat).toInstant();
            }
            case POINT, MULTIPOINT, LINESTRING, MULTILINESTRING, POLYGON, MULTIPOLYGON -> {
                GeoHolder geo = new GeoHolder(value, crs, GeoFormat.GEO_JSON);
                checkGeometry(normalizeGeo, type, geo);
                yield geo;
            }
        };
    }

    private Object assignTo(Double value, Type type) {
        return switch (type) {
            case DECIMAL, STRING, RAW, KEYWORD -> value;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, String.valueOf(value));
        };
    }

    private Object assignTo(Date value, Type type) {
        return switch (type) {
            case STRING, RAW, KEYWORD -> value;
            case INSTANT -> value.toInstant();
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot parse date to other type");
        };

    }

    private Object assignTo(Instant value, Type type) {
        return switch (type) {
            case STRING, RAW, INSTANT, KEYWORD -> value;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot parse date to other type");
        };
    }

    private Object assignTo(GeoHolder value, Type type, boolean normalizeGeo) {
        return switch (type) {
            case POINT, MULTIPOINT, LINESTRING, MULTILINESTRING, POLYGON, MULTIPOLYGON -> {
                checkGeometry(normalizeGeo, type, value);
                yield value;
            }
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, String.valueOf(value));
        };
    }

    private void assignOrGetError(AttributeDefDetailsDto attribute,
            ItemRecord itemRecord,
            Map<String, Object> values,
            List<ExtractedMessage> errors, boolean normalizeGeo) {
        Object value = itemRecord.values().get(attribute.technicalName);
        Type type = attribute.field.getType();

        try {
            values.put(attribute.technicalName, assignTo(value, type, normalizeGeo, attribute.field.crs));

        } catch (BusinessException | IllegalArgumentException | DateTimeParseException exception) {
            FileImportDto.ParamsTypeError paramsError = new FileImportDto.ParamsTypeError(attribute.name);
            errors.add(new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.FORMAT, paramsError));
        }
    }

    private void checkGeometry(boolean normalizeGeo, Type type, GeoHolder geoHolder) {
        // Converting only multi geometry and when a normalisation is asked
        if (normalizeGeo & type.isMultiGeo()) {
            geoHolder.transformToMulti();
        }

        if (geoHolder.getType() != type) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Type not corresponding: " + geoHolder.getType() + "/" + type);
        }
    }

}