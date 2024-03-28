package com.provoly.common.exec;

import java.util.Objects;

public class ParameterValueDto {

    private final String name;
    private final String value;

    public ParameterValueDto(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParameterValueDto that = (ParameterValueDto) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
