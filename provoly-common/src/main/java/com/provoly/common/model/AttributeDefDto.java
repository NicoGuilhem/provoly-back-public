package com.provoly.common.model;

import java.util.UUID;

public class AttributeDefDto {
    public UUID id;
    public String name;
    public String technicalName;
    public UUID field;
    public UUID category;
    public boolean multiValued;
    public String slug;

    @Override
    public String toString() {
        return name;
    }
}
