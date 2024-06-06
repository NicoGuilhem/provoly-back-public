package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.ComposedConditionDto;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleBasedLayout;

@ApplicationScoped
public class KuzzleLayout extends KuzzleBasedLayout {
    public static final String COLLECTION_NAME = "provoly";
    public static final String TIMEZONE_IDENTIFIER = "Z";

    private ElasticSupport elasticSupport;

    public KuzzleLayout(ElasticSupport elasticSupport) {
        this.elasticSupport = elasticSupport;
    }

    @Override
    public String buildAttributeRootPath(AttributeDefDetailsDto attribute) {
        return attribute.getTechnicalName();
    }

    @Override
    public String buildAttributePath(AttributeDefDetailsDto attribute) {
        return attribute.getTechnicalName();
    }

    @Override
    public String buildElasticMetadataPath(MetadataDefDto metadataDef) {
        return "%s.%s".formatted(META_FIELD_NAME, metadataDef.name);
    }

    @Override
    public String getIdPath() {
        return "%s.%s".formatted(META_FIELD_NAME, MetadataSystem.ID.getName());
    }

    @Override
    public ComposedConditionDto getLayoutConditions(OClassDetailsDto classDto) {
        return null;
    }

    @Override
    public Item convertToItem(Map<String, Object> hit, OClassDetailsDto oClass, UUID datasetversionId) {
        var id = getId(hit);
        Item item = new Item(new ItemId(id), oClass);

        // Load attributes : Based on attributes defined in Class. Everything else is ignored
        for (AttributeDefDetailsDto attributeDef : oClass.getAttributes()) {
            var attributeValue = hit.get(attributeDef.getTechnicalName());
            if (attributeValue == null)
                continue;
            var attributeSimple = item.getAttributeSimple(attributeDef.getTechnicalName());
            elasticSupport.extractAttributeValue(attributeSimple, attributeValue);
        }
        return item;
    }

    @Override
    public String buildAggregateAttributePath(AttributeDefDetailsDto attribute) {
        return buildAttributePath(attribute);
    }

    private String getId(Map<String, Object> hit) {
        Map<String, Object> itemMetaMap = (Map<String, Object>) hit.getOrDefault(META_FIELD_NAME, new HashMap<>());
        return itemMetaMap.get(MetadataSystem.ID.getName()).toString();
    }
}
