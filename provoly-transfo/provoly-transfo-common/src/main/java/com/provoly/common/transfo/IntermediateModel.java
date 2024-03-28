package com.provoly.common.transfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.provoly.common.model.FieldDto;
import com.provoly.common.model.Type;

public class IntermediateModel {

    private final Map<String, IntermediateModelAttribute> attributes = new HashMap<>();

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public void addAttribute(String name, FieldDto fieldDto) {
        attributes.put(name, new IntermediateModelAttribute(name, Type.from(fieldDto.type)));
    }

    public Type getAttributeType(String name) {
        return attributes.get(name).type();
    }

    public Collection<IntermediateModelAttribute> getAttributes() {
        return attributes.values();
    }

}
