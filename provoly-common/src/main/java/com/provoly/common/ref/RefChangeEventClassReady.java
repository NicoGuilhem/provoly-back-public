package com.provoly.common.ref;

import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventClassReady extends RefChangeEventClass {

    @JsonCreator
    public RefChangeEventClassReady(OClassDetailsDto oClassDetails) {
        super(Type.CLASS_READY, oClassDetails);
    }
}
