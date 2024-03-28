package com.provoly.common.transfo;

import java.util.Collection;
import java.util.UUID;

public class TransfoErrorLoopInTask extends TransfoError {

    private final Collection<UUID> ids;

    public TransfoErrorLoopInTask(Collection<UUID> ids) {
        this.ids = ids;
    }

    public Collection<UUID> getIds() {
        return ids;
    }
}
