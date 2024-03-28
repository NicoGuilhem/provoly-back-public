package com.provoly.common.transfo;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SlotDto {
    private final UUID id;
    private final int slot;

    @JsonCreator
    @Default
    public SlotDto(UUID id, int slot) {
        this.id = id;
        this.slot = slot;
    }

    @Override
    public String toString() {
        return "SlotDto{" +
                "id=" + id +
                ", slot='" + slot + '\'' +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public int getSlot() {
        return slot;
    }
}
