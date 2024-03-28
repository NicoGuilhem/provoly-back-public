package com.provoly.common.search;

import java.util.ArrayList;
import java.util.List;

public class ComposedConditionDto extends ConditionDto {
    public List<ConditionDto> composed = new ArrayList<>();

    @Override
    public String toString() {
        return "{" +
                "type: \"" + type + '"' +
                ", composed: " + composed + "} ";
    }
}
