package com.provoly.virt.storage.postgis;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;

@ApplicationScoped
class PostgisSupport {

    static final String COLUMN_NAME_ID = "provoly_id";
    static final String COLUMN_NAME_DATASET_VERSION = "provoly_dataset_version";

    static final String AGGREGATION_KEY = "aggreg";
    static final String VALUE_KEY = "value";
    static final String GROUPBY_KEY = "groupBy";

    static final Collection<String> TECHNICAL_COLUMNS = List.of(COLUMN_NAME_ID, COLUMN_NAME_DATASET_VERSION);
    private static final int MAX_NAME_LENGTH = 63;

    public String getTableName(OClassDetailsDto oClass) {
        return convertToPostgisName(oClass.getSlug());
    }

    public String getColumnName(AttributeDefDetailsDto attr) {
        return convertToPostgisName(attr.slug);
    }

    private String convertToPostgisName(String slug) {
        // We add a "c_" in front of slug, because postgres doesn't allow name starting by a number
        // "-" are not allowed in postgres names
        // Lowercase sha1 part
        var name = "c_" + slug.replace("-", "_").toLowerCase();
        var nameLength = Math.min(name.length(), MAX_NAME_LENGTH);
        return name.substring(0, nameLength);
    }
}
