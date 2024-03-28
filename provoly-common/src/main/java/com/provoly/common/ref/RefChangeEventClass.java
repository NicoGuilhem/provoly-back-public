package com.provoly.common.ref;

import com.provoly.common.model.OClassDetailsDto;

public class RefChangeEventClass extends RefChangeEvent {

    protected final OClassDetailsDto oClassDetails;

    public RefChangeEventClass(Type type, OClassDetailsDto oClassDetails) {
        super(type);
        this.oClassDetails = oClassDetails;
    }

    public OClassDetailsDto getoClassDetails() {
        return oClassDetails;
    }
}
