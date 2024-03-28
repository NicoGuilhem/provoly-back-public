package com.provoly.ref.user;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.searchrequest.Sort;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class SortMapper {

    @AfterMapping
    void checkAttributeIsNotGeoPoint(@MappingTarget Sort sort) {
        if (sort.getAttribute().getField().getType().isGeo()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Impossible to sort on geopoint field type");
        }
    }
}
