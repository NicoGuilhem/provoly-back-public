package com.provoly.common.model.field;

import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FieldNumericDto.class, name = "integer"),
        @JsonSubTypes.Type(value = FieldDateDto.class, name = "instant"),
        @JsonSubTypes.Type(value = FieldNumericDto.class, name = "long"),
        @JsonSubTypes.Type(value = FieldDto.class, name = "raw"),
        @JsonSubTypes.Type(value = FieldDto.class, name = "string"),
        @JsonSubTypes.Type(value = FieldDto.class, name = "keyword"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "Point"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "Multipoint"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "LineString"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "MultiLineString"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "Polygon"),
        @JsonSubTypes.Type(value = FieldGeoDto.class, name = "MultiPolygon"),
        @JsonSubTypes.Type(value = FieldDecimalDto.class, name = "decimal"),
})
public class FieldDto {
    private UUID id;
    private String name;
    private String type;
    private String slug;

    public FieldDto() {
    }

    @Default
    @JsonCreator
    public FieldDto(UUID id, String name, String type, String slug) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.slug = slug;
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

    public boolean isGeo() {
        Type currentType = Type.from(type);
        return currentType == Type.MULTILINESTRING || currentType == Type.LINESTRING || currentType == Type.MULTIPOINT
                || currentType == Type.POINT || currentType == Type.MULTIPOLYGON || currentType == Type.POLYGON;
    }

    public boolean isNumeric() {
        Type currentType = Type.from(type);
        return currentType == Type.INTEGER;
    }

    public Type getType() {
        return type == null ? null : Type.from(type);
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    // Allow to overide field Validation like for FieldGeoDto
    public void checkField() throws BusinessException {
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
