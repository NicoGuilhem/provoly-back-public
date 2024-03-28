package com.provoly.common.transfo;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class LinkDto {
    private final SlotDto start;
    private final SlotDto end;

    @Default
    @JsonCreator
    public LinkDto(SlotDto start, SlotDto end) {
        this.start = start;
        this.end = end;
    }

    public LinkDto(UUID startNodeId, int startSlot, UUID endNodeId, int endSlot) {
        this(new SlotDto(startNodeId, startSlot), new SlotDto(endNodeId, endSlot));
    }

    @Override
    public String toString() {
        return "LinkDto{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

    public SlotDto getStart() {
        return start;
    }

    public SlotDto getEnd() {
        return end;
    }
}
