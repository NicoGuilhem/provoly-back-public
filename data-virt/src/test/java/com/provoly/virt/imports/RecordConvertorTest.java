package com.provoly.virt.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.*;

import com.provoly.common.Storage;
import com.provoly.common.imports.ExtractedMessage;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.imports.model.ConversionResult;
import com.provoly.virt.imports.model.ItemRecord;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordConvertorTest {
    RecordConvertor recordConvertor;

    Logger log;

    OClassDetailsDto oClassDetailsDto;

    String point;

    String multiPoint;

    String lineString;

    String multiLineString;

    String polygon;

    String multiPolygon;

    @BeforeEach
    public void initializeClass() {
        log = Logger.getLogger(RecordConvertorTest.class);
        recordConvertor = new RecordConvertor(log);

        List<AttributeDefDetailsDto> attributeDefDetailsDtoList = new ArrayList<>();
        for (Type type : Type.values()) {
            FieldDto fieldDto = new FieldDto();
            fieldDto.type = type.getName().toLowerCase();
            fieldDto.id = UUID.randomUUID();
            if (type.isGeo()) {
                fieldDto.crs = "EPSG:4326";
            }

            AttributeDefDetailsDto att = new AttributeDefDetailsDto();
            att.name = type.getName().toLowerCase();
            att.technicalName = type.getName().toLowerCase();
            att.field = fieldDto;
            //TODO ajouter category random -> pas d'appel à ref
            attributeDefDetailsDtoList.add(att);
        }

        oClassDetailsDto = new OClassDetailsDto(UUID.randomUUID(), "name", "slug", "", attributeDefDetailsDtoList,
                Storage.ELASTIC,
                List.of());

        point = "{ \"type\": \"Point\", \"coordinates\": [30.0, 10.0] }";
        multiPoint = "{ \"type\": \"MultiPoint\", \"coordinates\": [[30.0, 10.0], [30.0, 10.0], [30.0, 10.0]] }";
        lineString = "{ \"type\": \"LineString\", \"coordinates\": [ [30.0, 10.0], [10.0, 30.0] ] }";
        multiLineString = "{ \"type\": \"MultiLineString\", \"coordinates\": [ [ [30.0, 10.0], [10.0, 30.0], [40.0, 40.0] ], [ [30.0, 10.0], [10.0, 30.0], [40.0, 40.0] ] ] }";
        polygon = "{ \"type\": \"Polygon\", \"coordinates\": [ [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ] ] }";
        multiPolygon = "{ \"type\": \"MultiPolygon\", \"coordinates\": [ [ [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ], [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ] ] ] }";
    }

    @Test
    void validate_withCorrectValue_shouldReturnOK() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", "String");
        values.put("keyword", "keyword");
        values.put("integer", 1);
        values.put("long", 1L);
        values.put("decimal", 1.5);
        values.put("raw", "rawwwwwww");
        values.put("instant", Instant.now());
        values.put("point", point);
        values.put("multipoint", multiPoint);
        values.put("linestring", lineString);
        values.put("multilinestring", multiLineString);
        values.put("polygon", polygon);
        values.put("multipolygon", multiPolygon);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);

        assertThat(result.messages()).isEmpty();
    }

    @Test
    void validate_withCorrectValueAndValidConversion_shouldReturnOK() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", "String");
        values.put("keyword", "keyword");
        values.put("integer", "1");
        values.put("long", 1);
        values.put("decimal", 1L);
        values.put("raw", "rawwwwwww");
        values.put("instant", Date.from(Instant.now()));
        values.put("point", point);
        values.put("multipoint", multiPoint);
        values.put("linestring", lineString);
        values.put("multilinestring", multiLineString);
        values.put("polygon", polygon);
        values.put("multipolygon", multiPolygon);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);

        assertThat(result.messages()).isEmpty();
    }

    @Test
    void validate_withAllIncorrectValue_shouldReturnErrorsExceptFor_String_Raw_Keyword() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", 1);
        values.put("keyword", 2);
        values.put("integer", "String");
        values.put("long", "String");
        values.put("decimal", Instant.now());
        values.put("raw", point);
        values.put("instant", "String");
        values.put("point", Instant.now());
        values.put("multipoint", point);
        values.put("linestring", 1);
        values.put("multilinestring", 1L);
        values.put("polygon", "String");
        values.put("multipolygon", 1.5);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);
        assertThat(result.messages()).hasSize(10);
    }

    @Test
    void validate_withAllEmptyString_shouldReturnOk() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", "");
        values.put("keyword", "");
        values.put("integer", "");
        values.put("long", "");
        values.put("decimal", "");
        values.put("raw", "");
        values.put("instant", "");
        values.put("point", "");
        values.put("multipoint", "");
        values.put("linestring", "");
        values.put("multilinestring", "");
        values.put("polygon", "");
        values.put("multipolygon", "");

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);

        assertThat(result.messages()).isEmpty();
    }

    @Test
    void validate_withAllNull_shouldReturnError() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", null);
        values.put("keyword", null);
        values.put("integer", null);
        values.put("long", null);
        values.put("decimal", null);
        values.put("raw", null);
        values.put("instant", null);
        values.put("point", null);
        values.put("multipoint", null);
        values.put("linestring", null);
        values.put("multilinestring", null);
        values.put("polygon", null);
        values.put("multipolygon", null);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);

        assertThat(result.messages()).hasSize(1);
    }

    @Test
    void validateHeader_withEveryAttribute_returnNoErrors() {
        List<String> values = new ArrayList<>();
        values.add("string");
        values.add("keyword");
        values.add("integer");
        values.add("long");
        values.add("decimal");
        values.add("raw");
        values.add("instant");
        values.add("point");
        values.add("multipoint");
        values.add("linestring");
        values.add("multilinestring");
        values.add("polygon");
        values.add("multipolygon");

        List<ExtractedMessage> result = recordConvertor.validateAttributeNames(values,
                oClassDetailsDto.getAttributes().stream().map(attribute -> attribute.name));

        assertThat(result).isEmpty();
    }

    @Test
    void validateHeader_withMissingAttribute_returnSuccess() {
        List<String> values = new ArrayList<>();
        values.add("string");
        values.add("keyword");
        values.add("integer");
        values.add("long");
        values.add("point");
        values.add("multipoint");
        values.add("linestring");
        values.add("multilinestring");
        values.add("polygon");
        values.add("multipolygon");

        List<ExtractedMessage> result = recordConvertor.validateAttributeNames(values,
                oClassDetailsDto.getAttributes().stream().map(attribute -> attribute.name));

        assertThat(result).isEmpty();
    }

    @Test
    void validateHeader_withWrongAttribute_returnWarning() {
        List<String> values = new ArrayList<>();
        values.add("ssstring");
        values.add("keyword");
        values.add("integer");
        values.add("long");
        values.add("point");
        values.add("multipoint");
        values.add("linestring");
        values.add("multilinestring");
        values.add("polygon");
        values.add("multipolygon");
        values.add("string");
        values.add("raw");
        values.add("instant");
        values.add("decimal");

        List<ExtractedMessage> result = recordConvertor.validateAttributeNames(values,
                oClassDetailsDto.getAttributes().stream().map(attribute -> attribute.name));

        assertEquals(1, result.size());
        assertEquals(MessageLevel.WARNING, result.get(0).messageLevel());
    }

    @Test
    void validate_withBadGeoValue_returnOneError() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", "String");
        values.put("keyword", "keyword");
        values.put("integer", "1");
        values.put("long", 1);
        values.put("decimal", 1L);
        values.put("raw", "rawwwwwww");
        values.put("instant", Date.from(Instant.now()));
        values.put("point", lineString);
        values.put("multipoint", multiPoint);
        values.put("linestring", lineString);
        values.put("multilinestring", multiLineString);
        values.put("polygon", polygon);
        values.put("multipolygon", multiPolygon);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto);

        assertThat(result.messages()).hasSize(1);
    }

    @Test
    void validate_withGeoNormalization_returnOK() {
        Map<String, Object> values = new HashMap<>();
        /*
         * values.put("string", "String");
         * values.put("keyword", "keyword");
         * values.put("integer", "1");
         * values.put("long", 1);
         * values.put("decimal", 1L);
         * values.put("raw", "rawwwwwww");
         * values.put("instant", Date.from(Instant.now()));
         * values.put("point", point);
         * values.put("multipoint", point);
         */
        values.put("linestring", lineString);
        values.put("multilinestring", lineString);
        values.put("polygon", polygon);
        values.put("multipolygon", polygon);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto, true);

        assertThat(result.messages()).isEmpty();
    }

    @Test
    void validate_withValidFormatForAttributes_returnOK() {
        Map<String, Object> values = new HashMap<>();
        values.put("string", Instant.now());
        values.put("keyword", 2);
        values.put("integer", "1");
        values.put("long", 1);
        values.put("decimal", 1L);
        values.put("raw", "rawwwwwww");
        values.put("instant", Date.from(Instant.now()));
        values.put("point", point);
        values.put("multipoint", point);
        values.put("linestring", lineString);
        values.put("multilinestring", lineString);
        values.put("polygon", polygon);
        values.put("multipolygon", polygon);

        ItemRecord itemRecord = new ItemRecord("1", values);

        ConversionResult result = recordConvertor.convert(itemRecord, oClassDetailsDto, true);

        assertThat(result.messages()).isEmpty();
    }

}
