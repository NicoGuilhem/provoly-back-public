package com.provoly.common.ref;

import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventClassUpdated extends RefChangeEventClass {

    @JsonCreator
    public RefChangeEventClassUpdated(OClassDetailsDto oClassDetails) {
        super(Type.CLASS_UPDATED, oClassDetails);
    }
}
