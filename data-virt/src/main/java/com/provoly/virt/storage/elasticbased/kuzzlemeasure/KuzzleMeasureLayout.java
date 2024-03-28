package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

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
import com.provoly.common.search.AndConditionDto;
import com.provoly.common.search.ComposedConditionDto;
import com.provoly.common.search.MetadataConditionDto;
import com.provoly.common.search.Operator;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleBasedLayout;
import com.provoly.virt.storage.elasticbased.KuzzleClient;

import org.jboss.logging.Logger;

@ApplicationScoped
public class KuzzleMeasureLayout extends KuzzleBasedLayout {

    public static final String MEASURE_COLLECTION = "measures";

    public static final String ATTRIBUTE_PATH = "values";
    public static final String MEASURE_NAME = "type";
    public static final String ASSET_MODEL = "asset.model";
    public static final String ID = "_id";
    public static final String MEASURED_AT = "measuredAt";
    public static final String ASSET = "asset";
    public static final String ASSET_ID = "asset_id";
    public static final String METADATA = "metadata";

    private ElasticSupport elasticSupport;
    private KuzzleClient kuzzleClient;
    private Logger log;

    public KuzzleMeasureLayout(KuzzleClient kuzzleClient, ElasticSupport elasticSupport, Logger log) {
        this.kuzzleClient = kuzzleClient;
        this.log = log;
        this.elasticSupport = elasticSupport;
    }

    @Override
    public String buildAttributeRootPath(AttributeDefDetailsDto attribute) {
        log.debugf("get root path for attribute %s", attribute.name);
        if (attributeIsMappedAsMetadata(attribute)) {
            return "%s.%s".formatted(ASSET, METADATA);
        }
        return switch (attribute.technicalName) {
            case ASSET_ID -> ASSET;
            case MEASURED_AT -> "";
            default -> ATTRIBUTE_PATH;
        };

    }

    @Override
    public String buildAttributePath(AttributeDefDetailsDto attribute) {
        log.debugf("get path for attribute %s", attribute.name);

        if (attributeIsMappedAsMetadata(attribute)) {
            return "%s.%s.%s".formatted(ASSET, METADATA, attribute.technicalName);
        }

        return switch (attribute.technicalName) {
            case ASSET_ID -> "%s.%s".formatted(ASSET, ID);
            case MEASURED_AT -> MEASURED_AT;
            default -> "%s.%s".formatted(ATTRIBUTE_PATH, attribute.technicalName);
        };
    }

    @Override
    public String buildElasticMetadataPath(MetadataDefDto metadataDef) {
        if (metadataDef.name.equals(MetadataSystem.MEASURE_NAME.getName())) {
            return MEASURE_NAME;
        }
        if (metadataDef.name.equals(MetadataSystem.ASSET_MODEL.getName())) {
            return ASSET_MODEL;
        }
        return null;
    }

    @Override
    public String getIdPath() {
        return ID;
    }

    @Override
    public ComposedConditionDto getLayoutConditions(OClassDetailsDto classDto) {
        MetadataValueReadDto metadataValueReadDtoAsset = getMetadataValue(classDto, MetadataSystem.ASSET_MODEL.getName());
        MetadataValueReadDto metadataValueReadDtoMeasure = getMetadataValue(classDto, MetadataSystem.MEASURE_NAME.getName());
        MetadataConditionDto assetModelCondition = new MetadataConditionDto(MetadataSystem.ASSET_MODEL,
                metadataValueReadDtoAsset.getValue(),
                Operator.EQUALS);
        MetadataConditionDto measureNameCondition = new MetadataConditionDto(MetadataSystem.MEASURE_NAME,
                metadataValueReadDtoMeasure.getValue(),
                Operator.EQUALS);
        ComposedConditionDto composedConditionDto = new AndConditionDto();
        composedConditionDto.composed.add(assetModelCondition);
        composedConditionDto.composed.add(measureNameCondition);
        return composedConditionDto;
    }

    @Override
    public Item convertToItem(Map<String, Object> hit, OClassDetailsDto oClass, UUID dataserversionId) {
        var id = hit.get(ID).toString();
        Item item = new Item(new ItemId(dataserversionId, id), oClass);

        elasticSupport.setAttributeValue(oClass, item, MEASURED_AT, hit.get(MEASURED_AT));

        for (Map.Entry<String, Object> prop : ((Map<String, Object>) hit.get(ATTRIBUTE_PATH)).entrySet()) {
            elasticSupport.setAttributeValue(oClass, item, prop.getKey(), prop.getValue());
        }

        Map<String, Object> asset = (Map<String, Object>) hit.get(ASSET);

        elasticSupport.setAttributeValue(oClass, item, ASSET_ID, asset.get(ID));
        for (Map.Entry<String, Object> prop : ((Map<String, Object>) asset.get(METADATA)).entrySet()) {
            elasticSupport.setAttributeValue(oClass, item, prop.getKey(), prop.getValue());
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

    @Override
    public String buildAggregateAttributePath(AttributeDefDetailsDto attribute) {
        return buildAttributePath(attribute);
    }

    private boolean attributeIsMappedAsMetadata(AttributeDefDetailsDto attribute) {
        var assetPropMapping = getMappingOf(kuzzleClient.getMeasureMapping(), ASSET);
        Map<String, Object> assetMetadataMapping = getMappingOf(assetPropMapping, METADATA);
        for (var entry : assetMetadataMapping.entrySet()) {
            if (attribute.technicalName.equals(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> getMappingOf(Map<String, Object> mapping, String key) {
        return (Map<String, Object>) ((Map<String, Object>) mapping.get(key)).get("properties");
    }

}
