package com.provoly.virt.event;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.storage.StorageInitEventListener;
import com.provoly.virt.storage.elasticbased.KuzzleClient;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.runtime.StartupEvent;

import org.elasticsearch.client.RestClient;

@ApplicationScoped
public final class ApplicationStartEventListener {

    private final List<StorageInitEventListener> storageInitEventListeners;
    private final DataSourceSupport dataSourceSupport;

    public ApplicationStartEventListener(
            RestClient elasticRestClient,
            KuzzleClient kuzzleClient,
            @Any Instance<StorageInitEventListener> storageInitEventListeners,
            DataSourceSupport dataSourceSupport) {
        this.dataSourceSupport = dataSourceSupport;
        atLeastOneStorage(elasticRestClient, kuzzleClient);
        this.storageInitEventListeners = storageInitEventListeners
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private void atLeastOneStorage(RestClient elasticRestClient, KuzzleClient kuzzleClient) {
        if (elasticRestClient == null && !isPostgisDatasourceConfigured() && !kuzzleClient.isConfigured()) {
            throw new ApplicationFailedToStart("At least one storage has to be configured (elasticsearch, postgis or kuzzle).");
        }
    }

    public void onInitEvent(@Observes StartupEvent ev) {
        storageInitEventListeners.forEach(listener -> listener.onInitEvent(ev));
    }

    private static class ApplicationFailedToStart extends BusinessException {
        public ApplicationFailedToStart(String message) {
            super(ErrorCode.FORBIDDEN, message);
        }
    }

    private boolean isPostgisDatasourceConfigured() {
        return !dataSourceSupport.getInactiveNames().contains(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
    }
}
