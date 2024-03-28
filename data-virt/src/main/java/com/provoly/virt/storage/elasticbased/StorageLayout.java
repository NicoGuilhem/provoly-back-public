package com.provoly.virt.storage.elasticbased;

import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.search.Direction;

import co.elastic.clients.elasticsearch._types.SortOrder;

public abstract class StorageLayout {

    public static final String META_FIELD_NAME = "metadata";
    public static final String ATTRIBUTE_FIELD_NAME = "attributes";
    public static final String SIMPLE_ITEM_PREFIX = "SIMPLE_";
    public static final String MULTI_ITEM_PREFIX = "MULTI_";
    public static final String AGGS = "aggs";
    public static final String GROUP_BY = "group-by";
    public static final String OPERATION_AGGS = "function-aggs";
    public static final String KEY_ORDER_NAME = "_key";
    public static final String DEFAULT_ORDER_NAME = "_count";
    public static final String RESULT_KEY = "result";
    public static final String TEXT_SUFFIX_ENDS_WITH = "ends_with";

    public abstract String buildAttributeRootPath(AttributeDefDetailsDto attribute);

    public abstract String buildAttributePath(AttributeDefDetailsDto attribute);

    public abstract String buildElasticMetadataPath(MetadataDefDto metadataDef);

    public abstract String getIdPath();

    public abstract String buildAggregateAttributePath(AttributeDefDetailsDto attribute);

    public SortOrder getSortDirection(Direction direction) {
        return direction == Direction.desc ? SortOrder.Desc : SortOrder.Asc;
    }
}
