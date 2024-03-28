package com.provoly.common.ref;

import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventClassCreated extends RefChangeEventClass {

    @JsonCreator
    public RefChangeEventClassCreated(OClassDetailsDto oClassDetails) {
        super(Type.CLASS_CREATED, oClassDetails);
    }
}
