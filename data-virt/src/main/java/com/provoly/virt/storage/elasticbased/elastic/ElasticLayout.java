package com.provoly.virt.storage.elasticbased.elastic;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.SortDto;
import com.provoly.common.search.SortType;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.MetadataValueDto;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.StorageLayout;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;

@ApplicationScoped
class ElasticLayout extends StorageLayout {
    private static final String ATTRIBUTE_FIELD_SUFFIX = "attribute";
    public static final String AGGREGATION_SUFFIX = "aggregation";

    private MetadataRefService metadataRefService;
    private DataVirtProperties dataVirtProperties;
    private StorageSupport storageSupport;

    public ElasticLayout(@RestClient MetadataRefService metadataRefService,
            DataVirtProperties dataVirtProperties, StorageSupport storageSupport) {
        this.metadataRefService = metadataRefService;
        this.dataVirtProperties = dataVirtProperties;
        this.storageSupport = storageSupport;
    }

    @Override
    public String buildAttributeRootPath(AttributeDefDetailsDto attribute) {
        return ATTRIBUTE_FIELD_NAME + "." + getAttributePrefix(attribute) + attribute.getSlug();
    }

    @Override
    public String buildElasticMetadataPath(MetadataDefDto metadataDef) {
        return META_FIELD_NAME + "." + metadataDef.type + "_" + metadataDef.slug;
    }

    @Override
    public String getIdPath() {
        return META_FIELD_NAME + "."
                + buildElasticMetadataName(metadataRefService.get(MetadataSystem.ID.getId()));
    }

    @Override
    public String buildAttributePath(AttributeDefDetailsDto attribute) {
        String elasticPath = buildAttributeRootPath(attribute);
        String elasticField = buildElasticAttributeName(attribute);
        return elasticPath + "." + elasticField;
    }

    @Override
    public String buildAggregateAttributePath(AttributeDefDetailsDto attribute) {
        String elasticPath = buildAttributeRootPath(attribute);
        String elasticField = buildElasticAttributeName(attribute);
        if (attribute.getField().type.equals(Type.KEYWORD.getName())) {
            return "%s.%s.%s".formatted(elasticPath, elasticField, AGGREGATION_SUFFIX);
        }
        return "%s.%s".formatted(elasticPath, elasticField);
    }

    public String buildElasticAttributeName(AttributeDefDetailsDto attributeDef) {
        return attributeDef.getField().slug + "_" + ATTRIBUTE_FIELD_SUFFIX;
    }

    public String buildElasticAttributeName(AttributeSimpleValue attribute) {
        return attribute.getSlugField() + "_" + ATTRIBUTE_FIELD_SUFFIX;
    }

    public String buildElasticMetadataName(MetadataValueDto metadataValueDto) {
        return metadataValueDto.getDefinition().type + "_" + metadataValueDto.getDefinition().slug;
    }

    public String buildElasticMetadataName(MetadataDefDto metadataDefDto) {
        return metadataDefDto.type + "_" + metadataDefDto.slug;
    }

    public void prepareRequest(BulkRequest.Builder request) {
        if (dataVirtProperties.elasticEnableImmediateRefreshPolicy()) {
            request.refresh(Refresh.WaitFor);
        }
    }

    public void prepareRequest(DeleteRequest.Builder request) {
        if (dataVirtProperties.elasticEnableImmediateRefreshPolicy()) {
            request.refresh(Refresh.WaitFor);
        }
    }

    public boolean containsItemId(SortDto effectiveSorts) {
        return (effectiveSorts != null) &&
                (effectiveSorts.type().equals(SortType.ITEM_ID)
                        || effectiveSorts.attribute().equals(MetadataSystem.ID.getId()));
    }

    private String getAttributePrefix(AttributeDefDetailsDto attribute) {
        return attribute.isMultiValued() ? MULTI_ITEM_PREFIX : SIMPLE_ITEM_PREFIX;
    }

    public String getElasticFieldPathForMetadata(UUID metadataId) {
        return buildElasticMetadataPath(metadataRefService.get(metadataId));
    }

    public String getElasticFieldPathForAttribute(AttributeDefDetailsDto attribute) {
        if (attribute.getField().getType().isGeo()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Sort is not allowed on geopoint field.");
        }
        return buildAttributePath(attribute);
    }

    public String getSortPath(OClassDetailsDto classDto,
            SortDto sort) {
        return switch (sort.type()) {
            case METADATA -> getElasticFieldPathForMetadata(sort.attribute());
            case ATTRIBUTE ->
                getElasticFieldPathForAttribute(storageSupport.getAttributeById(classDto, sort.attribute()));
            case ITEM_ID -> getIdPath();
        };
    }
}