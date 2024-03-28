package com.provoly.virt.storage.elasticbased;

import java.util.Arrays;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.virt.storage.StorageSupport;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;

@ApplicationScoped
public class SearchQueryBuilder {

    private Logger log;

    private MetadataRefService metadataService;

    private StorageLayout storageLayout;

    private StorageSupport storageSupport;

    Predicate<AttributeDefDetailsDto> isKeywordType = attributeDefDetailsDto -> attributeDefDetailsDto.field
            .getType() == Type.KEYWORD;

    public SearchQueryBuilder() {
    }

    public SearchQueryBuilder(Logger log,
            @RestClient MetadataRefService metadataService,
            StorageSupport support,
            StorageLayout storageLayout) {
        this.log = log;
        this.metadataService = metadataService;
        this.storageLayout = storageLayout;
        this.storageSupport = support;
    }

    public Query buildQuery(OClassDetailsDto classDto, ComposedConditionDto conditionDto) {
        var emptyCondition = new OrConditionDto();
        return buildQuery(classDto, conditionDto, emptyCondition);
    }

    public Query buildQuery(OClassDetailsDto classDto, ConditionDto conditionDto, ComposedConditionDto securityMetadata) {
        return buildQuery(classDto, conditionDto, securityMetadata, "");
    }

    private Query buildQuery(OClassDetailsDto classDto, ConditionDto conditionDto, ComposedConditionDto securityMetadata,
            String elasticPath) {

        return switch (conditionDto) {
            case TrueConditionDto ignored -> Query.of(q -> q.matchAll(m -> m));
            case OrConditionDto orCondition -> buildQuery(classDto, orCondition, securityMetadata, elasticPath);
            case AndConditionDto andCondition -> buildQuery(classDto, andCondition, securityMetadata, elasticPath);
            case AttributeConditionDto attributeConditionDto -> {
                var attribute = storageSupport.getAttributeById(classDto, attributeConditionDto.getAttribute());
                if (attribute.multiValued) {
                    yield buildQueryMulti(classDto, attribute, attributeConditionDto, securityMetadata); // TODO : Should take a path
                } else {
                    yield buildQuerySimple(classDto, attribute, attributeConditionDto, securityMetadata);
                }
            }
            case MetadataConditionDto metadataCondition -> buildQuery(metadataCondition, elasticPath);
            default ->
                throw new IllegalStateException("Unknown Condition type " + conditionDto.type); // TODO to string for all request conditions
        };
    }

    private Query buildQuery(OClassDetailsDto classDto, OrConditionDto conditionDto, ComposedConditionDto securityMetadata,
            String elasticPath) {
        var condition = new BoolQuery.Builder();
        conditionDto.composed
                .forEach(c -> condition.should(buildQuery(classDto, c, securityMetadata, elasticPath)));
        return Query.of(q -> q.bool(condition.build()));
    }

    private Query buildQuery(OClassDetailsDto classDto, AndConditionDto conditionDto, ComposedConditionDto securityMetadata,
            String elasticPath) {
        var condition = new BoolQuery.Builder();
        conditionDto.composed
                .forEach(c -> condition.must(buildQuery(classDto, c, securityMetadata, elasticPath)));
        return Query.of(q -> q.bool(condition.build()));
    }

    private Query buildQueryMulti(OClassDetailsDto classDto, AttributeDefDetailsDto attributeDef,
            AttributeConditionDto conditionDto,
            ComposedConditionDto securityMetadata) {
        var path = StorageLayout.ATTRIBUTE_FIELD_NAME
                + "." + StorageLayout.MULTI_ITEM_PREFIX + attributeDef.slug;

        // TODO : Choose best score mode
        return Query.of(q -> q
                .nested(n -> n
                        .path(path)
                        .query(buildQuerySimple(classDto, attributeDef, conditionDto, securityMetadata))
                        .scoreMode(ChildScoreMode.Avg)
                        .ignoreUnmapped(true)));
    }

    private Query buildQuerySimple(OClassDetailsDto classDto, AttributeDefDetailsDto attribute,
            AttributeConditionDto conditionDto,
            ComposedConditionDto securityMetadata) {
        String elasticPath = storageLayout.buildAttributeRootPath(attribute);
        validateTypeFieldValue(attribute.field.type, conditionDto.getValue());

        if (conditionDto.getUpperValue() != null) {
            validateTypeFieldValue(attribute.field.type, conditionDto.getUpperValue());
        }

        var attributeQuery = leafBuildQuery(conditionDto, attribute);
        if (securityMetadata.composed.isEmpty()) {
            return attributeQuery;
        } else {
            return Query.of(q -> q
                    .bool(b -> b
                            .must(attributeQuery)
                            .mustNot(buildQuery(classDto, securityMetadata, null, elasticPath))));
        }
    }

    private Query leafBuildQuery(AttributeConditionDto conditionDto, AttributeDefDetailsDto attribute) {
        var elasticFieldPath = storageLayout.buildAttributePath(attribute);

        // spotless:off
        String value = conditionDto.getValue();
        return switch (conditionDto.getOperator()) {
            case EQUALS -> buildEquals(attribute, elasticFieldPath, value, false);
            case I_EQUALS -> buildEquals(attribute, elasticFieldPath, value, true);
            case NOT_EQUALS -> buildNotEquals(attribute, elasticFieldPath, value, false);
            case I_NOT_EQUALS -> buildNotEquals(attribute, elasticFieldPath, value, true);
            case CONTAINS -> buildContains(attribute, elasticFieldPath, value, false);
            case I_CONTAINS -> buildContains(attribute, elasticFieldPath, value, true);
            case START_WITH -> buildStartWith(attribute, elasticFieldPath, value, false);
            case I_START_WITH -> buildStartWith(attribute, elasticFieldPath, value, true);
            case END_WITH -> buildEndsWith(attribute, elasticFieldPath, value, false);
            case I_END_WITH -> buildEndsWith(attribute, elasticFieldPath, value, true);
            case GREATER_THAN -> Query.of(q -> q
                    .range(r -> r
                            .field(elasticFieldPath)
                            .gt(JsonData.of(value))));
            case LOWER_THAN -> Query.of(q -> q
                    .range(r -> r
                            .field(elasticFieldPath)
                            .lt(JsonData.of(value))));

            case INSIDE -> Query.of(q -> q
                    .range(r -> r
                            .field(elasticFieldPath)
                            .from(value)
                            .to(conditionDto.getUpperValue())));
            case OUTSIDE -> Query.of(q -> q
                    .bool(b -> b
                            .mustNot(c -> c
                                    .range(r -> r
                                            .field(elasticFieldPath)
                                            .from(value)
                                            .to(conditionDto.getUpperValue())))));
            case EXISTS -> Query.of(q -> q
                    .exists(e -> e
                            .field(elasticFieldPath)));
            case DISTANCE -> Query.of(q -> q
                    .geoDistance(d -> d
                            .field(elasticFieldPath)
                            .location(l -> l.text(conditionDto.getLocation()))
                            .distance(value)));
            case INTERSECTS -> {
                String geoToString = conditionDto.getValue();
                    yield Query.of(
                            q -> q.geoShape(
                                    b -> b.field(elasticFieldPath)
                                            .shape(s -> s.relation(GeoShapeRelation.Intersects)
                                                    .shape(JsonData.fromJson(geoToString)))));

            }
        };
        // spotless:on
    }

    private Query buildEquals(AttributeDefDetailsDto attribute, String elasticFieldPath, String value, boolean caseSensitive) {
        return isKeywordType.test(attribute) ? buildEqualsKeyword(elasticFieldPath, value, caseSensitive)
                : buildEqualsText(elasticFieldPath, value);
    }

    private Query buildNotEquals(AttributeDefDetailsDto attribute, String elasticFieldPath, String value,
            boolean caseSensitive) {
        return isKeywordType.test(attribute) ? buildNotEqualsKeyword(elasticFieldPath, value, caseSensitive)
                : buildNotEqualsText(elasticFieldPath, value);
    }

    private Query buildContains(AttributeDefDetailsDto attribute, String elasticFieldPath, String value,
            boolean caseSensitive) {
        return isKeywordType.test(attribute) ? buildWildcards(elasticFieldPath, "*" + value + "*", caseSensitive)
                : buildContainsText(elasticFieldPath, value);
    }

    private Query buildStartWith(AttributeDefDetailsDto attribute, String elasticFieldPath, String value,
            boolean caseSensitive) {
        return isKeywordType.test(attribute) ? buildWildcards(elasticFieldPath, value + "*", caseSensitive)
                : buildStartsWithText(elasticFieldPath, value);

    }

    private Query buildEndsWith(AttributeDefDetailsDto attribute, String elasticFieldPath, String value,
            boolean caseSensitive) {
        return isKeywordType.test(attribute) ? buildWildcards(elasticFieldPath, "*" + value, caseSensitive)
                : buildEndsWithText(elasticFieldPath, value);
    }

    private Query buildWildcards(String elasticFieldPath, String conditionDto, boolean caseInsensitive) {
        return Query.of(q -> q
                .wildcard(w -> w
                        .field(elasticFieldPath)
                        .value(conditionDto)
                        .caseInsensitive(caseInsensitive)));
    }

    private Query buildContainsText(String elasticFieldPath, String conditionDto) {
        var queries = Arrays.stream(conditionDto.split(" "))
                .map(sub -> Query.of(q -> q
                        .queryString(t -> t
                                .fields(elasticFieldPath)
                                .analyzeWildcard(true)
                                .allowLeadingWildcard(true)
                                .defaultOperator(Operator.And)
                                .query("*%s*".formatted(sub)))))
                .toList();

        return Query.of(q -> q
                .bool(b -> b
                        .must(queries)));

    }

    private Query buildStartsWithText(String elasticFieldPath, String conditionDto) {
        return Query.of(q -> q
                .matchPhrasePrefix(t -> t
                        .field(elasticFieldPath)
                        .query(conditionDto)));
    }

    private Query buildEndsWithText(String elasticFieldPath, String conditionDto) {
        return Query.of(q -> q
                .matchPhrasePrefix(t -> t
                        .field("%s.%s".formatted(elasticFieldPath, StorageLayout.TEXT_SUFFIX_ENDS_WITH))
                        .query(conditionDto)));
    }

    private Query buildNotEqualsKeyword(String elasticFieldPath, String conditionDto, boolean caseInsensitive) {
        return Query.of(q -> q
                .bool(b -> b
                        .mustNot(mn -> mn
                                .term(t -> t
                                        .field(elasticFieldPath)
                                        .value(conditionDto)
                                        .caseInsensitive(caseInsensitive)))));
    }

    private Query buildNotEqualsText(String elasticFieldPath, String conditionDto) {
        return Query.of(q -> q
                .bool(b -> b
                        .mustNot(mn -> mn
                                .match(t -> t
                                        .field(elasticFieldPath)
                                        .query(conditionDto)))));
    }

    private Query buildEqualsKeyword(String elasticFieldPath, String conditionDto, boolean caseInsensitive) {
        return Query.of(q -> q
                .term(t -> t
                        .field(elasticFieldPath)
                        .value(conditionDto)
                        .caseInsensitive(caseInsensitive)));
    }

    private Query buildEqualsText(String elasticFieldPath, String conditionDto) {
        return Query.of(q -> q
                .match(t -> t
                        .field(elasticFieldPath)
                        .query(conditionDto)));
    }

    private Query buildQuery(MetadataConditionDto conditionDto, String elasticPath) {

        if (MetadataSystem.ID.is(conditionDto.getMetadata())) {
            return Query.of(q -> q
                    .ids(i -> i
                            .values(conditionDto.getValue())));
        }

        var metadata = metadataService.get(conditionDto.getMetadata());
        String elasticFieldPath = (elasticPath.isEmpty() ? "" : elasticPath + ".")
                + storageLayout.buildElasticMetadataPath(metadata);
        // spotless:off
        return switch (conditionDto.getOperator()) {
            case EQUALS -> buildEqualsKeyword(elasticFieldPath, conditionDto.getValue(), false);
            case I_EQUALS -> buildEqualsKeyword(elasticFieldPath, conditionDto.getValue(), true);
            case NOT_EQUALS -> buildNotEqualsKeyword(elasticFieldPath, conditionDto.getValue(), false);
            case I_NOT_EQUALS -> buildNotEqualsKeyword(elasticFieldPath, conditionDto.getValue(), true);
            case EXISTS -> Query.of(q -> q
                    .exists(e -> e
                            .field(elasticFieldPath)));
            default -> throw new BusinessException(ErrorCode.TECHNICAL,
                    "Operator not supported : " + conditionDto.getOperator());
        };
        // spotless:on
    }

    public void validateTypeFieldValue(String type, String value) {
        log.tracef("Check if type of value %s match with type %s", value, type);
        try {
            switch (Type.from(type)) {
                case INTEGER -> Integer.parseInt(value);
                case LONG -> Long.parseLong(value);
                case DECIMAL -> Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Invalid type - unable to convert value " + value + " to " + type);
        }
    }
}
