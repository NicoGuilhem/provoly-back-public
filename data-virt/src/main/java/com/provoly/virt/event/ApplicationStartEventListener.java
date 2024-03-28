package com.provoly.virt.event;

import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.storage.StorageInitEventListener;
import com.provoly.virt.storage.elasticbased.KuzzleClient;

import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.runtime.StartupEvent;

import org.elasticsearch.client.RestClient;

@ApplicationScoped
public final class ApplicationStartEventListener {

    private final List<StorageInitEventListener> storageInitEventListeners;

    //FIXME Je n'aime pas du tout cette façon de faire le contrôle, il va falloior trouver mieux.
    public ApplicationStartEventListener(
            RestClient elasticRestClient,
            DataSource pgDataSource,
            KuzzleClient kuzzleClient,
            @Any Instance<StorageInitEventListener> storageInitEventListeners) {
        atLeastOneStorage(elasticRestClient, pgDataSource, kuzzleClient);
        this.storageInitEventListeners = storageInitEventListeners
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private void atLeastOneStorage(RestClient elasticRestClient,
            DataSource pgDataSource, KuzzleClient kuzzleClient) {
        if (elasticRestClient == null && pgDataSource instanceof UnconfiguredDataSource && kuzzleClient == null) {
            throw new ApplicationFailedToStart("At least one storage has to be defined.");
        }
    }

    public void onInitEvent(@Observes StartupEvent ev) {
        storageInitEventListeners.forEach(listener -> listener.onInitEvent(ev));
    }

    public static class ApplicationFailedToStart extends BusinessException {

        public ApplicationFailedToStart(String message) {
            super(ErrorCode.FORBIDDEN, message);
        }
    }
}
