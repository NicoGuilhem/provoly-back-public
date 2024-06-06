package com.provoly.virt.storage.postgis;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.NotSupportedStorageException;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.ProvolySpanManager;
import com.provoly.virt.storage.StorageSupport;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

class PostgisSelectQueryBuilder {

    private StringBuilder sql;

    private final PostgisSupport postgisSupport;
    private final ObjectMapper mapper;

    private final StorageSupport storageSupport;

    private final Logger log;

    private OClassDetailsDto oClassDetailsDto;

    private Consumer<PreparedStatement> preparedStatementConsumer;
    private AtomicInteger index = new AtomicInteger(1);

    public PostgisSelectQueryBuilder(PostgisSupport postgisSupport, ObjectMapper mapper, Logger log,
            StorageSupport storageSupport, OClassDetailsDto oClassDetailsDto, ProvolySpanManager spanManager) {
        this.postgisSupport = postgisSupport;
        this.mapper = mapper;
        this.log = log;
        this.sql = new StringBuilder();
        this.storageSupport = storageSupport;
        this.oClassDetailsDto = oClassDetailsDto;
        preparedStatementConsumer = ps -> {
        };
    }

    private void addParam(Object value) {
        preparedStatementConsumer = preparedStatementConsumer.andThen(ps -> {
            try {
                ps.setObject(index.getAndIncrement(), value);
            } catch (SQLException e) {
                throw new BusinessException(ErrorCode.TECHNICAL, "Unable to generate request", e);
            }
        });
    }

    public PostgisSelectQueryBuilder select(String valueFieldName, AggregateOperation aggregateOperation,
            String aggregateFieldName, String groupBy) {

        log.trace("SELECT ");
        log.tracef("%s as " + PostgisSupport.VALUE_KEY, valueFieldName);
        sql.append("SELECT ");

        if (aggregateFieldName != null) {
            sql.append(aggregateFieldName).append(" as " + PostgisSupport.AGGREGATION_KEY + ", ");
            log.tracef("%s AS AGGREG ", aggregateFieldName);
        } else {
            sql.append(" 'result' as " + PostgisSupport.AGGREGATION_KEY + ", ").append(System.lineSeparator());
        }

        if (groupBy != null) {
            sql.append(groupBy).append(" as ").append(PostgisSupport.GROUPBY_KEY).append(", ")
                    .append(System.lineSeparator());
            log.tracef("%s as group by", groupBy);
        }

        String operation = switch (aggregateOperation) {
            case COUNT -> "COUNT(" + valueFieldName + ")";
            case MAX -> "MAX( CAST (" + valueFieldName + " AS DOUBLE PRECISION))";
            case MIN -> "MIN( CAST( " + valueFieldName + " AS DOUBLE PRECISION))";
            case AVG -> "AVG(" + valueFieldName + ")";
            case SUM -> "SUM(CAST(" + valueFieldName + " AS DOUBLE PRECISION))";
            case CARDINALITY -> "COUNT (DISTINCT " + valueFieldName + ")";
            case MEDIAN -> "PERCENTILE_CONT(0.5) WITHIN GROUP(order by " + valueFieldName + ")";
            case Q1 -> "PERCENTILE_DISC(0.25) WITHIN GROUP(order by " + valueFieldName + ")";
            case Q3 -> "PERCENTILE_DISC(0.75) WITHIN GROUP(order by " + valueFieldName + ")";
            case EXTENT -> "ST_Extent(" + valueFieldName + ")";
        };

        sql.append(operation);
        sql.append(" AS " + PostgisSupport.VALUE_KEY + " ").append(System.lineSeparator());
        return this;
    }

    public PostgisSelectQueryBuilder select() {
        log.trace("SELECT ALL");
        this.sql.append("SELECT *").append(System.lineSeparator());
        return this;
    }

    public PostgisSelectQueryBuilder count() {
        log.trace("COUNT");
        this.sql.append("SELECT COUNT(*) as full_count").append(System.lineSeparator());
        return this;
    }

    public PostgisSelectQueryBuilder select(List<String> columnsName) {
        String result = String.join(", ", columnsName);
        log.trace("SELECT %s".formatted(result));

        this.sql.append("SELECT %s ".formatted(result));
        return this;
    }

    public PostgisSelectQueryBuilder with(String aggregatedByName, Double interval) {
        if (interval == null) {
            return this;
        }

        prepareSerie(aggregatedByName);
        sql.append(interval).append(")),");
        prepareBaseQuery();
        return this;
    }

    public PostgisSelectQueryBuilder with(String aggregatedByName, DateInterval interval) {
        if (interval == null) {
            return this;
        }

        prepareSerie(aggregatedByName);
        sql.append("'1 ").append(sqlDateIntervalFrom(interval)).append("')),");
        prepareBaseQuery();
        return this;
    }

    public PostgisSelectQueryBuilder interval(Double interval, AggregateOperation operation, String groupBy) {
        if (interval == null) {
            return this;
        }
        String operationSql = getIntervalOperation(operation);

        sql.append(")");
        sql.append(System.lineSeparator());
        sql.append(System.lineSeparator());
        sql.append("SELECT COALESCE(").append(operationSql).append("value), 0) as ").append(PostgisSupport.VALUE_KEY)
                .append(",");
        sql.append(System.lineSeparator());
        if (groupBy != null) {
            sql.append("groupBy as groupBy, ");
        }
        sql.append("CAST (serie.generate_series AS DOUBLE PRECISION)  as " + PostgisSupport.AGGREGATION_KEY + " FROM serie");
        sql.append(System.lineSeparator());
        sql.append(
                "LEFT JOIN base_query on (base_query." + PostgisSupport.AGGREGATION_KEY
                        + " >= serie.generate_series and base_query." + PostgisSupport.AGGREGATION_KEY
                        + " < serie.generate_series +")
                .append(interval).append(")");
        sql.append(System.lineSeparator());
        sql.append("group by serie.generate_series ");
        if (groupBy != null) {
            sql.append(", groupBy ");
        }
        sql.append("order by serie.generate_series");
        return this;
    }

    public PostgisSelectQueryBuilder dateInterval(DateInterval interval, AggregateOperation operation) {
        if (interval == null) {
            return this;
        }

        String operationSql = getIntervalOperation(operation);

        sql.append(")");
        sql.append(System.lineSeparator());
        sql.append(System.lineSeparator());
        sql.append("SELECT COALESCE(").append(operationSql).append("value), 0) as ").append(PostgisSupport.VALUE_KEY)
                .append(",");
        sql.append(System.lineSeparator());
        sql.append("serie.generate_series  as " + PostgisSupport.AGGREGATION_KEY + " FROM serie");
        sql.append(System.lineSeparator());
        sql.append(
                "LEFT JOIN base_query on (base_query." + PostgisSupport.AGGREGATION_KEY
                        + " >= serie.generate_series and base_query." + PostgisSupport.AGGREGATION_KEY
                        + " < serie.generate_series + interval '1 ")
                .append(sqlDateIntervalFrom(interval)).append("')");
        sql.append(System.lineSeparator());
        sql.append("group by serie.generate_series order by serie.generate_series");
        return this;
    }

    public PostgisSelectQueryBuilder from() {
        log.trace("FROM");
        sql.append("FROM %s WHERE 1 = 1 ".formatted(postgisSupport.getTableName(oClassDetailsDto)))
                .append(System.lineSeparator());
        return this;
    }

    public PostgisSelectQueryBuilder limit(int limit) {
        log.trace("LIMIT");
        sql.append(" LIMIT %s".formatted(limit));
        return this;
    }

    public PostgisSelectQueryBuilder offset(long offset) {
        log.trace("OFFSET");
        sql.append(" OFFSET %s".formatted(offset));
        return this;
    }

    public PostgisSelectQueryBuilder where(ConditionDto condition, OrConditionDto securityMetadata) {
        if (condition == null) {
            return this;
        }
        log.trace(" AND ");
        sql.append("AND ");
        buildConditionSql(condition, null); // FIXME : Security
        return this;
    }

    public PostgisSelectQueryBuilder orderBy(SortDto sort) {
        if (sort == null) {
            return this;
        }
        log.trace("ORDER BY");
        switch (sort.type()) {
            case ATTRIBUTE -> {
                AttributeDefDetailsDto attributeDefDetailsDto = oClassDetailsDto.getAttributes().stream()
                        .filter(attribute -> attribute.getId().equals(sort.attribute())).findAny()
                        .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN,
                                "No attribute with id %s in Oclass %s".formatted(sort.attribute(),
                                        oClassDetailsDto.getName())));
                sql.append(
                        " ORDER BY %s %s ".formatted(postgisSupport.getColumnName(attributeDefDetailsDto), sort.direction()));
            }
            case METADATA -> throw new NotSupportedStorageException("Postgis storage not support sort on metadata");
            case ITEM_ID -> sql.append(" ORDER BY %s %s".formatted(PostgisSupport.COLUMN_NAME_ID, sort.direction()));
        }
        sql.append(System.lineSeparator());
        return this;
    }

    public PostgisSelectQueryBuilder orderBy(SortAggregate sortAggregate, String aggregationParam) {
        if (sortAggregate == null || sortAggregate.direction() == null) {
            return this;
        }
        log.trace("ORDER BY");

        if (sortAggregate.orderBy() == OrderBy.VALUE) {
            sql.append(" ORDER BY ").append(PostgisSupport.VALUE_KEY).append(" ").append(sortAggregate.direction());
        } else {
            sql.append(" ORDER BY ").append(aggregationParam).append(" ").append(sortAggregate.direction());
        }

        return this;

    }

    public PostgisSelectQueryBuilder groupBy(String aggregateFieldName, String groupBy) {
        if (aggregateFieldName == null) {
            return this;
        }

        log.trace("GROUP BY");
        sql.append(" GROUP BY ").append(aggregateFieldName).append(" ");

        if (groupBy != null) {
            sql.append(", ").append(groupBy).append(" ");
        }
        return this;
    }

    private void buildConditionSql(ConditionDto condition, OrConditionDto securityMetadata) {
        switch (condition) {
            case TrueConditionDto t -> sql.append(" true ");
            case OrConditionDto orCondition -> buildConditionSql(orCondition, securityMetadata);
            case AndConditionDto andCondition -> buildConditionSql(andCondition, securityMetadata);
            case AttributeConditionDto attributeConditionDto -> {
                var attribute = storageSupport.getAttributeById(oClassDetailsDto, attributeConditionDto.getAttribute());
                if (attribute.isMultiValued()) {
                    throw new NotSupportedStorageException("Postgis storage not support multivalued attribute");
                } else {
                    buildConditionSql(attribute, attributeConditionDto, securityMetadata);
                }
            }
            case MetadataConditionDto metadataCondition -> {
                if (metadataCondition.getMetadata().equals(MetadataSystem.DATASET_VERSION.getId())) {
                    sql.append(" provoly_dataset_version = ? ").append(System.lineSeparator());
                    addParam(UUID.fromString(metadataCondition.getValue()));
                } else {
                    throw new NotSupportedStorageException("Postgis storage not support meta condition");
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + condition);
        }
    }

    private void buildComposedConditionSql(String operator, ComposedConditionDto condition,
            OrConditionDto securityMetadata) {
        sql.append(" ( ");
        for (int i = 0; i < condition.composed.size(); i++) {
            var c = condition.composed.get(i);
            buildConditionSql(c, securityMetadata);
            if (i < condition.composed.size() - 1) {
                sql.append(" %s ".formatted(operator));
            }
        }
        sql.append(" ) ");
    }

    private void buildConditionSql(AndConditionDto condition, OrConditionDto securityMetadata) {
        buildComposedConditionSql("AND", condition, securityMetadata);
    }

    private void buildConditionSql(OrConditionDto condition, OrConditionDto securityMetadata) {
        buildComposedConditionSql("OR", condition, securityMetadata);
    }

    private void buildConditionSql(AttributeDefDetailsDto attribute, AttributeConditionDto condition,
            OrConditionDto securityMetadata) {

        var columnName = postgisSupport.getColumnName(attribute);
        var value = typedValue(attribute, condition);

        switch (condition.getOperator()) {
            case EQUALS -> appendOperators("%s = ?".formatted(columnName), value);
            case I_EQUALS -> appendOperators("%s ilike ?".formatted(columnName), value);
            case NOT_EQUALS -> appendOperators("%s <> ?".formatted(columnName), value);
            case I_NOT_EQUALS -> appendOperators("%s not ilike ?".formatted(columnName), value);
            case CONTAINS -> appendOperators("%s like ?".formatted(columnName), "%%%s%%".formatted(value));
            case I_CONTAINS -> appendOperators("%s ilike ?".formatted(columnName), "%%%s%%".formatted(value));
            case START_WITH -> appendOperators("%s like ?".formatted(columnName), "%s%%".formatted(value));
            case I_START_WITH -> appendOperators("%s ilike ?".formatted(columnName), "%s%%".formatted(value));
            case END_WITH -> appendOperators("%s like ?".formatted(columnName), "%%%s".formatted(value));
            case I_END_WITH -> appendOperators("%s ilike ?".formatted(columnName), "%%%s".formatted(value));
            case GREATER_THAN -> appendOperators("%s > ?".formatted(columnName), value);
            case LOWER_THAN -> appendOperators("%s < ?".formatted(columnName), value);
            case INSIDE -> appendOperators(" ( %s > ? AND %s < ? )".formatted(columnName, columnName), value,
                    typedValue(attribute, condition.getUpperValue()));
            case OUTSIDE -> appendOperators(" ( %s < ? OR %s > ? )".formatted(columnName, columnName), value,
                    typedValue(attribute, condition.getUpperValue()));
            case EXISTS -> appendOperators(" is not null", columnName);
            //FIXME INSTANCE and INTERSECT are workaround
            //Other operator won't work at all.
            case DISTANCE -> appendOperators("st_distance(%s, 'SRID=%s;%s', true) <= ?"
                    .formatted(columnName, attribute.getField().checkAndExtractSRID(), condition.getLocation()),
                    Double.valueOf(condition.getValue()));
            case INTERSECTS -> appendOperators("st_intersects(%s, st_geomfromgeojson(?))".formatted(columnName), value);
        }

    }

    private void appendOperators(String operator, Object... values) {
        sql.append(operator).append(System.lineSeparator());
        Arrays.stream(values).forEach(this::addParam);
    }

    private Object typedValue(AttributeDefDetailsDto attribute, AttributeConditionDto conditionDto) {
        return switch (attribute.getField().getType()) {
            case INTEGER, LONG, DECIMAL, STRING, KEYWORD, RAW, INSTANT -> typedValue(attribute, conditionDto.getValue());
            default -> getGeoValue(attribute, conditionDto);
        };
    }

    private Object getGeoValue(AttributeDefDetailsDto attribute, AttributeConditionDto conditionDto) {
        if (conditionDto.getLocation() != null) {
            var geo = new GeoHolder(conditionDto.getLocation(), attribute.getField().crs, GeoFormat.WKT).toString();
            return typedValue(attribute, geo);
        }

        if (conditionDto.getValue() != null) {
            var geo = new GeoHolder(conditionDto.getValue(), attribute.getField().crs, GeoFormat.GEO_JSON).toString();
            return typedValue(attribute, geo);
        }
        throw new BusinessException(ErrorCode.TECHNICAL, "A geo value must be set on geoJson or location property");
    }

    private Object typedValue(AttributeDefDetailsDto attribute, String value) {
        return switch (attribute.getField().getType()) {
            case INTEGER -> Integer.valueOf(value);
            case LONG -> Long.valueOf(value);
            case DECIMAL -> Double.valueOf(value);
            case INSTANT -> Timestamp.from(ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant());
            default -> value;
            // FIXME Geometry handling is far from optimal
        };
    }

    private void prepareSerie(String aggregatedByName) {
        sql.append("WITH serie as ( ");
        sql.append("SELECT * FROM  generate_series(");
        sql.append("(SELECT MIN(").append(aggregatedByName).append(") FROM ")
                .append(postgisSupport.getTableName(oClassDetailsDto))
                .append(") ,");
        sql.append("(SELECT MAX(").append(aggregatedByName).append(") FROM ")
                .append(postgisSupport.getTableName(oClassDetailsDto))
                .append(") ,");
    }

    private void prepareBaseQuery() {
        sql.append(System.lineSeparator());
        sql.append("base_query as ( ");
        sql.append(System.lineSeparator());
    }

    public String toQuery() {
        return sql.toString();
    }

    public ResultSet executeQuery(PreparedStatement ps) throws SQLException {
        preparedStatementConsumer.accept(ps);
        return ps.executeQuery();
    }

    private String sqlDateIntervalFrom(DateInterval interval) {
        return switch (interval) {
            case SECOND -> "seconds";
            case MINUTE -> "minutes";
            case HOUR -> "hours";
            case DAY -> "days";
            case WEEK -> "weeks";
            case QUARTER -> "quarters";
            case YEAR -> "years";
        };
    }

    private String getIntervalOperation(AggregateOperation operation) {
        return switch (operation) {
            case COUNT, SUM -> "SUM(";
            case MAX -> "MAX(";
            case MIN -> "MIN(";
            case AVG -> "AVG(";
            case CARDINALITY -> "COUNT (DISTINCT ";
            case MEDIAN -> "PERCENTILE_CONT(0.5) WITHIN GROUP(order by ";
            case Q1 -> "PERCENTILE_CONT(0.25) WITHIN GROUP(order by ";
            case Q3 -> "PERCENTILE_CONT(0.75) WITHIN GROUP(order by ";
            case EXTENT -> "ST_Extent(";
        };
    }
}
