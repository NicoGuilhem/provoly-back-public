package com.provoly.virt.storage.elasticbased.elastic;

import static com.provoly.common.metadata.MetadataSystem.DATASET;
import static com.provoly.common.metadata.MetadataSystem.DATASET_VERSION;
import static com.provoly.virt.storage.elasticbased.elastic.ElasticLayout.AGGREGATION_SUFFIX;
import static com.provoly.virt.storage.elasticbased.elastic.ElasticLayout.TEXT_SUFFIX_ENDS_WITH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.ElasticType;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.storage.StorageModelService;
import com.provoly.virt.storage.StorageQualifier;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.analysis.Analyzer;
import co.elastic.clients.elasticsearch._types.analysis.Normalizer;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.indices.GetFieldMappingResponse;

@StorageQualifier(Storage.ELASTIC)
@ApplicationScoped
class ElasticModelService implements StorageModelService {

    private static final String ASCII_NORMALIZER = "ascii_normalizer";
    public static final String ASCII_ANALYZER = "ascii_analyzer";
    public static final String ENDS_WITH_ANALYZER = "ends_with_analyzer";
    private static final String NORMALIZER_ASCII = "asciifolding";

    private Logger log;
    private ElasticsearchClient elastic;

    public ElasticModelService(Logger log, ElasticsearchClient elastic) {
        this.log = log;
        this.elastic = elastic;
    }

    @Override
    public void createOClass(OClassDetailsDto oClass) {
        log.infof("Creating index for class %s", oClass.getName());

        String indexName = oClass.getSlug();
        try {
            if (indexExists(indexName)) {
                throw new BusinessException(ErrorCode.NAME_ALREADY_USED, "Index %s already exists".formatted(indexName));
            }
            log.infov("Creating index with name %s".formatted(indexName));
            var response = elastic
                    .indices()
                    .create(i -> i
                            .index(indexName)
                            .settings(s -> s.analysis(a -> a
                                    .analyzer(Map.of(ASCII_ANALYZER, buildAsciiAnalyser())) // use standard "asciifolding" to search on texts that contains accents, by writing input with or without accents
                                    .analyzer(Map.of(ENDS_WITH_ANALYZER, buildAsciiEndsWithAnalyser())) // use "reverse" filter to perform "ends_with" search on texts that contains accents
                                    .normalizer(Map.of(ASCII_NORMALIZER, buildAsciiNormalizer())) // use standard "asciifolding" to search on keywords that contains accents, by writing input with or without accents
                            ))
                            .mappings(m -> m.dynamicTemplates(buildMapping(oClass.getAttributes()))));

            if (!response.acknowledged()) {
                throw new IllegalStateException("Unable to create %s index".formatted(indexName));
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Error creating index : %s in %s".formatted(oClass.getName(), indexName), e);
        }
    }

    @Override
    public void updateOClass(OClassDetailsDto oClass) {
        log.infof("Updating index for class %s", oClass.getName());

        String indexName = oClass.getSlug();
        try {
            if (!indexExists(indexName)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Index " + indexName + " not exists");
            }

            log.infov("Update index with name {0}", indexName);
            var response = elastic.indices().putMapping(p -> p
                    .index(indexName)
                    .dynamicTemplates(buildMapping(oClass.getAttributes())));
            if (!response.acknowledged()) {
                throw new IllegalStateException("Unable to update " + indexName + " index");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error updating index : " + oClass.getName() + " in " + indexName,
                    e);
        }

    }

    @Override
    public void deleteOClass(OClassDetailsDto oClass) {
        log.warnf("Deleting index for class %s", oClass.getName());

        var indexName = oClass.getSlug();
        try {
            if (indexExists(indexName)) { // TODO : Strange, we silently ignoring if index not existing
                var response = elastic.indices().delete(d -> d.index(indexName));
                if (!response.acknowledged()) {
                    throw new IllegalStateException("Unable to delete " + indexName + " index");
                }
            } else {
                log.warnf("Index [%s] not found", indexName);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Error deleting index=" + indexName, e);
        }
    }

    @Override
    public void deleteDatasetVersion(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto) {
        try {
            DeleteByQueryRequest deleteQuery = new DeleteByQueryRequest.Builder()
                    .index(oClassDetailsDto.getSlug())
                    .query(Query.of(query -> query
                            .bool(bool -> bool
                                    .must(condition -> condition
                                            .term(term -> term
                                                    .field("metadata.UUID_" + DATASET_VERSION.getMetadata().slug)
                                                    .value(datasetVersionDto.getId().toString())))
                                    .must(condition -> condition
                                            .term(term -> term
                                                    .field("metadata.UUID_" + DATASET.getMetadata().slug)
                                                    .value(datasetVersionDto.getDataset().toString()))))))
                    .build();
            log.debug("Query for deleting dataset version items : %s".formatted(deleteQuery));
            elastic.deleteByQuery(deleteQuery);
        } catch (Exception e) {
            log.error("An error occurred while deleting dataset items %s".formatted(datasetVersionDto.getDataset()), e);
        }
    }

    private List<Map<String, DynamicTemplate>> buildMapping(List<AttributeDefDetailsDto> attributes) {
        var mapping = new ArrayList<Map<String, DynamicTemplate>>();
        mapping.add(buildDynamicTemplate("MULTI", ElasticType.NESTED));
        mapping.add(buildDynamicTemplate("SIMPLE", ElasticType.OBJECT));

        for (VariableType type : VariableType.values()) { // This is used for metadata
            mapping.add(buildDynamicTemplate(type.name(), VariableType.getElasticType(type)));
        }
        for (AttributeDefDetailsDto attribute : attributes) { // This is used for all attributes
            var elasticType = Type.valueOf(attribute.getField().type.toUpperCase()).getElasticType();
            mapping.add(buildDynamicTemplate(attribute.getField().slug, elasticType));
        }
        return mapping;
    }

    private static Analyzer buildAsciiAnalyser() {
        return Analyzer.of(analyzer -> analyzer.custom(c -> c
                .tokenizer("keyword")
                .filter(List.of(NORMALIZER_ASCII, "lowercase"))));
    }

    private static Analyzer buildAsciiEndsWithAnalyser() {
        return Analyzer.of(analyzer -> analyzer.custom(c -> c
                .tokenizer("keyword")
                .filter(List.of(NORMALIZER_ASCII, "lowercase", "reverse"))));
    }

    private static Normalizer buildAsciiNormalizer() {
        return Normalizer.of(normalizer -> normalizer
                .custom(custom -> custom.filter(List.of(NORMALIZER_ASCII))));
    }

    public boolean indexExists(String indexName) throws IOException {
        return elastic.indices().exists(i -> i.index(indexName)).value();
    }

    private Map<String, DynamicTemplate> buildDynamicTemplate(String startWith, ElasticType type) {
        return Map.of(startWith, DynamicTemplate.of(t -> t
                .match(startWith + "_*")
                .mapping(buildMappingPropertyFor(type))));
    }

    private Property buildMappingPropertyFor(ElasticType type) {
        return switch (type) {
            case TEXT -> Property.of(builder -> builder.text(t -> t
                    .analyzer(ASCII_ANALYZER)
                    .fields(Map.of(TEXT_SUFFIX_ENDS_WITH, Property.of(b -> b.text(et -> et.analyzer(ENDS_WITH_ANALYZER))))))); // used for ends_with search
            case KEYWORD -> Property.of(builder -> builder.keyword(k -> k
                    .normalizer(ASCII_NORMALIZER)
                    .fields(Map.of(AGGREGATION_SUFFIX, Property.of(b -> b.keyword(new KeywordProperty.Builder().build())))))); // Use to return exact keyword on aggregations
            case FLOAT -> Property.of(builder -> builder.float_(new FloatNumberProperty.Builder().build()));
            case DOUBLE -> Property.of(builder -> builder.double_(new DoubleNumberProperty.Builder().build()));
            case DATE -> Property.of(builder -> builder.date(new DateProperty.Builder().build()));
            case INTEGER -> Property.of(builder -> builder.integer(new IntegerNumberProperty.Builder().build()));
            case LONG -> Property.of(builder -> builder.long_(new LongNumberProperty.Builder().build()));
            case NESTED -> Property.of(builder -> builder.nested(new NestedProperty.Builder().build()));
            case OBJECT -> Property.of(builder -> builder.object(new ObjectProperty.Builder().build()));
            case GEOPOINT -> Property.of(builder -> builder.geoPoint(new GeoPointProperty.Builder().build()));
            case GEOSHAPE -> Property.of(builder -> builder.geoShape(new GeoShapeProperty.Builder().build()));
        };
    }

    public boolean isFieldMapped(OClassDetailsDto classDto, String elasticFieldPath) {
        GetFieldMappingResponse res;
        try {
            res = elastic.indices().getFieldMapping(m -> m.fields(elasticFieldPath).index(classDto.getSlug()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return !res.get(classDto.getSlug()).mappings().isEmpty();
    }

}