package com.provoly.common.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Type {
    STRING(ElasticType.TEXT, PostgisType.VARCHAR, TypeCategory.STRING, "string"),
    KEYWORD(ElasticType.KEYWORD, PostgisType.VARCHAR, TypeCategory.STRING, "keyword"),
    INTEGER(ElasticType.INTEGER, PostgisType.INTEGER, TypeCategory.NUMERIC, "integer"),
    LONG(ElasticType.LONG, PostgisType.BIGINT, TypeCategory.NUMERIC, "long"),
    DECIMAL(ElasticType.DOUBLE, PostgisType.DOUBLE, TypeCategory.NUMERIC, "decimal"),
    RAW(ElasticType.TEXT, PostgisType.VARCHAR, TypeCategory.STRING, "raw"),
    INSTANT(ElasticType.DATE, PostgisType.TIMESTAMP, TypeCategory.DATE, "instant"),
    POINT(ElasticType.GEOSHAPE, PostgisType.POINT, TypeCategory.GEO, "Point"),
    MULTIPOINT(ElasticType.GEOSHAPE, PostgisType.MULTIPOINT, TypeCategory.GEO, "MultiPoint"),
    LINESTRING(ElasticType.GEOSHAPE, PostgisType.LINESTRING, TypeCategory.GEO, "LineString"),
    MULTILINESTRING(ElasticType.GEOSHAPE, PostgisType.MULTILINESTRING, TypeCategory.GEO, "MultiLineString"),
    POLYGON(ElasticType.GEOSHAPE, PostgisType.POLYGON, TypeCategory.GEO, "Polygon"),
    MULTIPOLYGON(ElasticType.GEOSHAPE, PostgisType.MULTIPOLYGON, TypeCategory.GEO, "MultiPolygon");

    private final ElasticType elasticType;
    private final PostgisType postgisType;
    private final TypeCategory typeCategory;
    private final String name;

    Type(ElasticType elasticType, PostgisType postgisType, TypeCategory typeCategory, String name) {
        this.elasticType = elasticType;
        this.typeCategory = typeCategory;
        this.name = name;
        this.postgisType = postgisType;
    }

    public ElasticType getElasticType() {
        return this.elasticType;
    }

    public TypeCategory getTypeCategory() {
        return this.typeCategory;
    }

    public PostgisType getPostgisType() {
        return this.postgisType;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    public static Type from(String name) {
        return Type.valueOf(name.toUpperCase());
    }

    public boolean isNumeric() {
        return List.of(Type.INTEGER, Type.DECIMAL, Type.LONG).contains(this);
    }

    public boolean isGeo() {
        return typeCategory == TypeCategory.GEO;
    }

    public boolean isMultiGeo() {
        return this == MULTIPOINT || this == MULTILINESTRING || this == MULTIPOLYGON;
    }
}
