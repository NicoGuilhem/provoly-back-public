package com.provoly.virt.imports.indexers;

import java.util.List;

import com.provoly.virt.imports.model.ItemRecord;

public interface FileWalker extends AutoCloseable {

    ItemRecord next();

    boolean hasNext();

    List<String> getAttributes();

}
