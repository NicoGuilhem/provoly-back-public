package com.provoly.common.model;

public enum ElasticType {
    KEYWORD("keyword"),
    TEXT("text"),
    FLOAT("float"),
    DOUBLE("double"),
    DATE("date"),
    INTEGER("integer"),
    LONG("long"),
    NESTED("nested"),
    OBJECT("object"),
    GEOPOINT("geo_point"),
    GEOSHAPE("geo_shape");

    private String name;

    ElasticType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
