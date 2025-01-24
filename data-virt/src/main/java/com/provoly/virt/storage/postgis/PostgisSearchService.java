package com.provoly.virt.storage.postgis;

import static java.lang.Math.min;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.NotSupportedStorageException;
import com.provoly.common.item.CountDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.ProvolySpanManager;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.SearchAfterContext;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSearchService;
import com.provoly.virt.storage.StorageSupport;

import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.trace.Span;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.postgis.jdbc.jts.JtsGeometry;

@StorageQualifier(Storage.POSTGIS)
@ApplicationScoped
class PostgisSearchService implements StorageSearchService {

    private final Logger log;
    private final PostgisSupport postgisSupport;

    private final AgroalDataSource dataSource;

    private final ObjectMapper mapper;

    private StorageSupport storageSupport;

    private ProvolySpanManager spanManager;

    public PostgisSearchService(
            Logger log,
            PostgisSupport postgisSupport,
            Instance<AgroalDataSource> dataSource,
            ObjectMapper mapper,
            StorageSupport storageSupport,
            ProvolySpanManager spanManager) {
        this.log = log;
        this.postgisSupport = postgisSupport;
        if (dataSource.isResolvable()) {
            this.dataSource = dataSource.get();
        } else {
            this.dataSource = null;
        }
        this.mapper = mapper;
        this.storageSupport = storageSupport;
        this.spanManager = spanManager;
    }

    @Override
    public ItemsSearchResult search(OClassDetailsDto oClass, MonoClassRequestDto request, MonoClassContextRequest context) {
        log.infof("Start search request on %s class", oClass.getName());
        Span span = spanManager.generateSpan("Start request on Postgis",
                Map.of("request", request.toString()));
        long searchAfter = prepareSearchAfter(request);

        List<String> recoveredSpecifiedColumns = null;
        if (!context.requestedAttributes().isEmpty()) {
            recoveredSpecifiedColumns = convertToPostgisAttributes(context.requestedAttributes());
        }

        var sqlBuilder = buildSelectSql(oClass, request, context, request.getLimit(), searchAfter, recoveredSpecifiedColumns);
        try (var conn = dataSource.getConnection();
                var statement = conn.prepareStatement(sqlBuilder.toQuery());
                var rs = sqlBuilder.executeQuery(statement);) {

            long count;
            CountDto countDto = null;
            ItemsSearchResult itemsSearchResult = convertResultSetToItems(oClass, rs);
            if (request.isWithCount()) {
                count = getCount(conn, oClass, request, context);
                countDto = new CountDto(count, request.isWithCount());
                itemsSearchResult.setCount(Map.of(oClass.getId(), countDto));
            } else {
                count = searchAfter + request.getLimit();
            }

            itemsSearchResult.setSearchAfter(new SearchAfterContext(null,
                    List.of(String.valueOf(min(searchAfter + request.getLimit(), count)))));

            return itemsSearchResult;
            // TODO : Add relations

        } catch (SQLException e) {
            spanManager.recordException(span, e);
            log.errorf("Error during SQL execution: %s", e.getErrorCode());
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search in class Id : %s".formatted(oClass.getId()), e);
        } finally {
            span.end();
        }
    }

    private long getCount(Connection conn, OClassDetailsDto oClass, MonoClassRequestDto request,
            MonoClassContextRequest context) throws SQLException {
        var countQuery = count(oClass, request, context);
        try (var countSt = conn.prepareStatement(countQuery.toQuery());
                var countRs = countQuery.executeQuery(countSt)) {
            countRs.next();
            return countRs.getLong("full_count");
        } catch (SQLException e) {
            log.errorf("Error during SQL execution: %s", e.getErrorCode());
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search in class Id : %s".formatted(oClass.getId()), e);
        }
    }

    private int prepareSearchAfter(MonoClassRequestDto request) {
        var searchAfter = 0;
        if (request.getSort() != null && request.getSearchAfter() != null) {
            try {
                var searchAfterContext = storageSupport.getSearchAfterContext(request.getSearchAfter());
                searchAfter = Integer.parseInt(searchAfterContext.searchAfter().getFirst());
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.TECHNICAL, "could not read search After");
            }

        }
        return searchAfter;
    }

    private PostgisSelectQueryBuilder buildSelectSql(OClassDetailsDto oClass, MonoClassRequestDto request,
            MonoClassContextRequest context, int limit,
            long searchAfter, List<String> columnsName) {
        checkPostgisSupportedFeatures(request, context);

        var selectQueryBuilder = new PostgisSelectQueryBuilder(postgisSupport, mapper, log, storageSupport, oClass,
                spanManager);
        if (columnsName != null) {
            selectQueryBuilder.select(columnsName);
        } else {
            selectQueryBuilder.select();
        }

        return selectQueryBuilder
                .from()
                .where(request.getCondition(), null)
                .where(context.datasetsCondition(), null)
                .orderBy(request.getSort())
                .limit(limit)
                .offset(searchAfter);

    }

    private void checkPostgisSupportedFeatures(MonoClassRequestDto request, MonoClassContextRequest context) {
        if (request.getFullSearch() != null) {
            throw new NotSupportedStorageException("Postgis does not support fullSearch");
        }

        if (context.isWithSecurityConditions()) {
            throw new NotSupportedStorageException("Postgis does not support security conditions");
        }
    }

    private PostgisSelectQueryBuilder count(OClassDetailsDto oClass, MonoClassRequestDto request,
            MonoClassContextRequest context) {
        checkPostgisSupportedFeatures(request, context);

        var selectQueryBuilder = new PostgisSelectQueryBuilder(postgisSupport, mapper, log, storageSupport, oClass,
                spanManager);
        return selectQueryBuilder.count()
                .from()
                .where(request.getCondition(), null)
                .where(context.datasetsCondition(), null);

    }

    private ItemsSearchResult convertResultSetToItems(OClassDetailsDto oClass, ResultSet rs)
            throws SQLException {
        var span = spanManager.generateSpan("Transforming to items", Map.of("class", oClass.getName()));
        var result = new ItemsSearchResult();
        while (rs.next()) {
            var id = rs.getString(PostgisSupport.COLUMN_NAME_ID);
            var dataset = rs.getObject(PostgisSupport.COLUMN_NAME_DATASET_VERSION, UUID.class);
            var itemId = new ItemId(dataset, id);
            var item = new Item(itemId, oClass);
            List<AttributeDefDetailsDto> recoveredAttributes = getRecoveredAttributes(oClass, rs);
            for (AttributeDefDetailsDto attribute : recoveredAttributes) {
                Object value = rs.getObject(postgisSupport.getColumnName(attribute));

                if (value != null) {
                    value = switch (item.getAttributeSimple(attribute.getTechnicalName()).getFieldType().getTypeCategory()) {
                        case GEO -> {
                            if (value instanceof JtsGeometry jtsGeometry) {
                                yield new GeoHolder(jtsGeometry.getGeometry());
                            } else {
                                yield value;
                            }
                        }
                        case DATE -> ((Timestamp) value).toInstant();
                        default -> value;
                    };

                    item.getAttributeSimple(attribute.getTechnicalName()).setValue(value);
                }
            }
            result.add(item);
        }
        span.end();
        return result;
    }

    private List<AttributeDefDetailsDto> getRecoveredAttributes(OClassDetailsDto oClass, ResultSet rs) throws SQLException {
        List<String> recoveredColumnsName = new ArrayList<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            recoveredColumnsName.add(rs.getMetaData().getColumnName(i));
        }
        return oClass.getAttributes().stream()
                .filter(attributesName -> recoveredColumnsName.contains(postgisSupport.getColumnName(attributesName)))
                .toList();
    }

    private List<String> convertToPostgisAttributes(List<AttributeDefDetailsDto> attributesColumns) {
        List<String> columns = new ArrayList<>();
        attributesColumns.forEach(attribute -> columns.add(postgisSupport.getColumnName(attribute)));
        columns.addAll(List.of(PostgisSupport.COLUMN_NAME_ID, PostgisSupport.COLUMN_NAME_DATASET_VERSION));
        return columns;
    }

}
