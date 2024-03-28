package com.provoly.transfo;

import jakarta.persistence.*;

@Embeddable
public class Link {
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "start_node")),
            @AttributeOverride(name = "slot", column = @Column(name = "start_slot")),
    })
    private Slot start;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "end_node")),
            @AttributeOverride(name = "slot", column = @Column(name = "end_slot")),
    })
    private Slot end;

    public Link() {
        // Only for JPA
    }

    public Slot getStart() {
        return start;
    }

    public void setStart(Slot start) {
        this.start = start;
    }

    public Slot getEnd() {
        return end;
    }

    public void setEnd(Slot end) {
        this.end = end;
    }
}
