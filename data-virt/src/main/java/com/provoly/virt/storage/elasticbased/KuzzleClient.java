package com.provoly.virt.storage.elasticbased;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.DataVirtProperties;

import io.kuzzle.sdk.Kuzzle;
import io.kuzzle.sdk.coreClasses.SearchResult;
import io.kuzzle.sdk.protocol.WebSocket;
import io.quarkus.cache.CacheResult;

@ApplicationScoped
public class KuzzleClient {
    private Kuzzle kuzzle;

    private DataVirtProperties dataVirtProperties;

    public KuzzleClient(DataVirtProperties dataVirtProperties) {
        this.dataVirtProperties = dataVirtProperties;
        WebSocket ws = new WebSocket(dataVirtProperties.kuzzle().host().orElse("localhost"));
        this.kuzzle = new Kuzzle(ws);
        this.kuzzle.connect(); //Subsequent calls have no effect if the SDK is already connected.
    }

    public Kuzzle client() {
        return kuzzle;
    }

    public SearchResult kuzzleSearch(String index, String collection, Map<String, Object> query, int limit) {
        try {
            return kuzzle.getDocumentController().search(
                    index,
                    collection,
                    query,
                    null,
                    Integer.valueOf(limit)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }
    }

    public void createIndexAndCollection(String indexName, String collection, Map<String, Map<String, Object>> mapping) {
        try {
            kuzzle.getIndexController().create(indexName).get();
            kuzzle.getCollectionController().create(indexName, collection, mapping).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to create index %s".formatted(indexName), e);
        }
    }

    public boolean storageExists(String indexName) {
        try {
            return kuzzle.getIndexController().exists(indexName).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to check if index %s exists".formatted(indexName), e);
        }
    }

    @CacheResult(cacheName = "kuzzle-measures-mapping")
    public Map<String, Object> getMeasureMapping() {
        try {
            return (Map<String, Object>) kuzzle
                    .getCollectionController()
                    .getMapping(dataVirtProperties
                            .kuzzle()
                            .tenant()
                            .orElseThrow(() -> new BusinessException(ErrorCode.TECHNICAL,
                                    "Tenant name is mandatory to get measure mapping")),
                            "measures")
                    .get().get("properties");
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to get the measure collection mapping", e);
        }
    }

}
