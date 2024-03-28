package com.provoly.virt.storage;

import io.quarkus.runtime.StartupEvent;

public interface StorageInitEventListener {

    void onInitEvent(StartupEvent event);
}
