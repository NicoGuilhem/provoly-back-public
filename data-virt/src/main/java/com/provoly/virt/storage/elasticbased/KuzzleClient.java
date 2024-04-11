package com.provoly.virt.storage.elasticbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.storage.InsertionError;

import io.kuzzle.sdk.Kuzzle;
import io.kuzzle.sdk.coreClasses.SearchResult;
import io.kuzzle.sdk.coreClasses.maps.KuzzleMap;
import io.kuzzle.sdk.protocol.WebSocket;
import io.quarkus.cache.CacheResult;

import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleClient {

    private final Logger log;
    private final Kuzzle kuzzle;
    private final DataVirtProperties dataVirtProperties;

    public KuzzleClient(DataVirtProperties dataVirtProperties, Logger log) {
        this.log = log;
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

    public List<InsertionError> insertDocuments(String index, String collection, List<Map<String, Object>> documents) {
        try {
            var result = kuzzle.getDocumentController()
                    .mCreateOrReplace(index, collection, new ArrayList<>(documents))
                    .get();
            return convertKuzzleResponseToError(result);
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error while communicating with kuzzle", e);
        }
    }

    private List<InsertionError> convertKuzzleResponseToError(Map<String, ArrayList<Object>> result) {
        var errors = result.get("errors");
        if (errors == null) {
            return List.of();
        }
        var pryErrors = new ArrayList<InsertionError>();
        for (Object error : errors) {
            var kuzzleErrorMap = (KuzzleMap) error;
            log.error("Error while inserting document in Kuzzle : %s".formatted(kuzzleErrorMap));
            var documentId = kuzzleErrorMap.getMap("document").getString("_id");
            pryErrors.add(new InsertionError(kuzzleErrorMap.getString("reason"), documentId));
        }
        return pryErrors;
    }

}
