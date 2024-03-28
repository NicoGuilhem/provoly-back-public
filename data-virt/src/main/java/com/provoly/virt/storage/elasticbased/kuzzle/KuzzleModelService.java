package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.storage.StorageModelService;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.elasticbased.KuzzleClient;

import org.jboss.logging.Logger;

@StorageQualifier(Storage.KUZZLE)
@ApplicationScoped
class KuzzleModelService implements StorageModelService {

    @Inject
    Logger log;

    @Inject
    KuzzleClient kuzzleClient;

    private static final String ACTION_NOT_IMPLEMENTED = "This action is not implemented yet for storage KUZZLE";
    private static final String ACTION_UPDATED_CLASS = "Updating index for class %s";

    @Override
    public void createOClass(OClassDetailsDto oClass) {
        log.infof("Creating kuzzle index for class %s", oClass.getName());

        String indexName = oClass.getSlug();

        if (kuzzleClient.storageExists(indexName)) {
            throw new BusinessException(ErrorCode.NAME_ALREADY_USED, "Index %s already exists".formatted(indexName));
        }

        var mapping = Map.of("mappings", buildMapping(oClass.getAttributes()));
        log.infov("Creating index with name %s".formatted(indexName));
        kuzzleClient.createIndexAndCollection(indexName, "provoly", mapping);
    }

    private Map<String, Object> buildMapping(List<AttributeDefDetailsDto> attributes) { // TODO: better mapping management
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();

        for (var attribute : attributes) {
            mapping.put(attribute.technicalName,
                    Map.of("type", Type.from(attribute.field.type).getElasticType().name().toLowerCase()));
        }
        for (var system : MetadataSystem.values()) {
            metadata.put(system.getName(),
                    Map.of("type", VariableType.getElasticType(system.getMetadata().type).toString().toLowerCase()));
        }
        mapping.put("metadata", Map.of("properties", metadata));
        return Map.of("properties", mapping);
    }

    @Override
    public void updateOClass(OClassDetailsDto oClass) {
        log.infof(ACTION_UPDATED_CLASS, oClass.getName());

        throw new BusinessException(ErrorCode.FORBIDDEN, ACTION_NOT_IMPLEMENTED);

    }

    @Override
    public void deleteOClass(OClassDetailsDto oClass) {
        log.warnf(ACTION_UPDATED_CLASS, oClass.getName());

        throw new BusinessException(ErrorCode.FORBIDDEN, ACTION_NOT_IMPLEMENTED);
    }

    @Override
    public void deleteDatasetVersion(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto) {
        log.warnf(ACTION_UPDATED_CLASS, oClassDetailsDto.getName());

        throw new BusinessException(ErrorCode.FORBIDDEN, ACTION_NOT_IMPLEMENTED);
    }

}