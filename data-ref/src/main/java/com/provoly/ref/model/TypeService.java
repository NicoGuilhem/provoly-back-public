package com.provoly.ref.model;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.model.Type;
import com.provoly.common.model.TypeDto;

@ApplicationScoped
public class TypeService {

    public List<TypeDto> getAll() {

        return Stream.of(Type.values())
                .map(type -> new TypeDto(type, type.getTypeCategory()))
                .toList();
    }
}
