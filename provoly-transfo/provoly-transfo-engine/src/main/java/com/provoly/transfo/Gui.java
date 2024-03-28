package com.provoly.transfo;

import java.io.Serializable;
import java.util.Collection;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

import com.provoly.common.Default;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

@Embeddable
public class Gui implements Serializable {
    @ElementCollection
    private Collection<Integer> pos;
    @ElementCollection
    private Collection<Integer> size;

    @Default
    public Gui(Collection<Integer> pos, Collection<Integer> size) {
        if (pos == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pos shouldn't be null.");
        }
        if (size == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size shouldn't be null.");
        }
        this.pos = pos;
        this.size = size;
    }

    public Gui() {
    }

    public Collection<Integer> getPos() {
        return pos;
    }

    public Collection<Integer> getSize() {
        return size;
    }
}