package com.provoly.ref.customclass;

import java.util.List;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class CustomClassMapper {

    public abstract List<String> toListOfContent(List<CustomClass> customClass);

    public String map(CustomClass customClass) {
        return customClass.getContent();
    }

}
