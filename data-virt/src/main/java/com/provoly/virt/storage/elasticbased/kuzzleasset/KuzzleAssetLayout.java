package com.provoly.virt.storage.elasticbased.kuzzleasset;

import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleBasedLayout;

@ApplicationScoped
public class KuzzleAssetLayout extends KuzzleBasedLayout {

    public static final String ASSET_COLLECTION = "assets";

    public static final String ATTRIBUTE_PATH = "metadata";
    public static final String MODEL_REFERENCE = "reference";
    public static final String ID = "_id";
    public static final String MODEL = "model";

    private ElasticSupport elasticSupport;

    public KuzzleAssetLayout(ElasticSupport elasticSupport) {
        this.elasticSupport = elasticSupport;
    }

    @Override
    public String buildAttributeRootPath(AttributeDefDetailsDto attribute) {
        return ATTRIBUTE_PATH;
    }

    @Override
    public String buildAttributePath(AttributeDefDetailsDto attribute) {
        return "%s.%s".formatted(ATTRIBUTE_PATH, attribute.name);
    }

    @Override
    public String buildElasticMetadataPath(MetadataDefDto metadataDef) {
        if (metadataDef.name.equals(MetadataSystem.ASSET_MODEL.getName())) {
            return MODEL;
        }
        if (metadataDef.name.equals(MetadataSystem.ID.getName())) {
            return MODEL_REFERENCE;
        }
        return MODEL;
    }

    @Override
    public String getIdPath() {
        return ID;
    }

    @Override
    public String buildAggregateAttributePath(AttributeDefDetailsDto attribute) {
        return buildAttributePath(attribute);
    }

    @Override
    public ComposedConditionDto getLayoutConditions(OClassDetailsDto classDto) {
        MetadataValueReadDto metadataValueReadDto = getMetadataValue(classDto, MetadataSystem.ASSET_MODEL.getName());
        MetadataConditionDto assetModelCondition = new MetadataConditionDto(MetadataSystem.ASSET_MODEL,
                metadataValueReadDto.getValue(), Operator.EQUALS);
        ComposedConditionDto composedConditionDto = new AndConditionDto();
        composedConditionDto.composed.add(assetModelCondition);
        return composedConditionDto;
    }

    @Override
    public Item convertToItem(Map<String, Object> hit, OClassDetailsDto oClass,
            UUID datasetversion) {
        var id = hit.get(MODEL_REFERENCE).toString();
        Item item = new Item(new ItemId(datasetversion, id), oClass);

        // Load attributes : Based on attributes defined in Class. Everything else is ignored
        for (AttributeDefDetailsDto attributeDef : oClass.getAttributes()) {
            var attributeValue = ((Map<String, Object>) hit.get(ATTRIBUTE_PATH)).get(attributeDef.technicalName);
            if (attributeValue == null)
                continue;
            var attributeSimple = item.getAttributeSimple(attributeDef.technicalName);
            elasticSupport.extractAttributeValue(attributeSimple, attributeValue);
        }
        return item;
    }

    private MetadataValueReadDto getMetadataValue(OClassDetailsDto classDto, String metadataName) {
        return classDto.getMetadata()
                .stream()
                .filter(x -> x.getMetadataDef().name.equals(metadataName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Metadata value need to be defined."));
    }
}
