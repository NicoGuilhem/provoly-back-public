package com.provoly.common.ref;

import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventClassDeleted extends RefChangeEventClass {

    @JsonCreator
    public RefChangeEventClassDeleted(OClassDetailsDto oClassDetails) {
        super(Type.CLASS_DELETED, oClassDetails);
    }

}
