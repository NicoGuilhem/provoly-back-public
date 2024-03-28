package com.provoly.common.virt;

import com.provoly.common.imports.ImportsMessage;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirtChangeEventImportMessageCreated extends VirtChangeEvent {

    protected final ImportsMessage importsMessage;

    @JsonCreator
    public VirtChangeEventImportMessageCreated(ImportsMessage importsMessage) {
        super(Type.IMPORT_MESSAGE);
        this.importsMessage = importsMessage;
    }

    public ImportsMessage getImportsMessage() {
        return importsMessage;
    }
}