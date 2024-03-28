package com.provoly.virt.storage.elasticbased.elastic;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

@ApplicationScoped
public class ElasticFullTextQueryBuilder {

    @Inject
    ElasticSearchQueryBuilder queryBuilder;

    /**
     * Build an Elastic query base on fullSearchCondition and respecting securityMetadata
     *
     * @param oClass
     * @param fullSearchCondition
     * @param securityMetadata
     * @return The elasticQuery or null if no attribute are of same type of fullsearch condition
     */
    public Query buildQuery(OClassDetailsDto oClass, FullSearchConditionDto fullSearchCondition,
            ComposedConditionDto securityMetadata) {
        boolean isValueLong = isLong(fullSearchCondition);
        boolean isValueInt = isInteger(fullSearchCondition);

        var condition = new OrConditionDto();
        for (AttributeDefDetailsDto attribute : oClass.getAttributes()) {
            var field = attribute.field;
            if (field == null) {
                String msg = "Unable to found field " + attribute.field + " for attribute " + oClass.getName() + "@"
                        + attribute.name;
                throw new BusinessException(ErrorCode.TECHNICAL, msg);
            }
            if (isText(field)) {
                condition.composed.add(
                        new AttributeConditionDto(attribute.id, fullSearchCondition.getValue(), Operator.CONTAINS));
            }

            if (((field.type.equals("integer") && isValueInt) || (field.type.equals("long") && isValueLong))) {
                var attributeCondition = new AttributeConditionDto(attribute.id, fullSearchCondition.getValue(),
                        Operator.EQUALS);
                condition.composed.add(attributeCondition);
            }

            // TODO : Elastic type should be an enum : Delete it from database
            // TODO : Search for "date"
            // TODO : Search for "float/double"

        }
        return condition.composed.isEmpty() ? null
                : queryBuilder.buildQuery(oClass, condition, securityMetadata);
    }

    private Collection<String> textFields = Set.of("keyword", "string");

    private boolean isText(FieldDto field) {
        return textFields.contains(field.type);
    }

    private boolean isInteger(FullSearchConditionDto condition) {
        var strNum = condition.getValue();
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean isLong(FullSearchConditionDto condition) {
        var strNum = condition.getValue();
        try {
            Long.parseLong(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
