package com.provoly.common.model;

public enum PostgisType {
    VARCHAR("varchar"),
    DOUBLE("double precision"),
    BIGINT("bigint"),
    TIMESTAMP("timestamp"),
    INTEGER("integer"),
    GEOMETRY("geometry"),
    POINT("POINT"),
    MULTIPOINT("MULTIPOINT"),
    LINESTRING("LINESTRING"),
    MULTILINESTRING("MULTILINESTRING"),
    POLYGON("POLYGON"),
    MULTIPOLYGON("MULTIPOLYGON");

    private String type;

    PostgisType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getGeoType(int srid) {
        return "geometry(%s, %s)".formatted(type, srid);
    }
}
