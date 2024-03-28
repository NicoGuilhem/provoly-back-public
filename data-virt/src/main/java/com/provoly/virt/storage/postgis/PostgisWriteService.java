package com.provoly.virt.storage.postgis;

import static java.util.stream.Collectors.groupingBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.Item;
import com.provoly.virt.storage.InsertionError;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageWriteService;

import io.agroal.api.AgroalDataSource;

import org.jboss.logging.Logger;

@StorageQualifier(Storage.POSTGIS)
@ApplicationScoped
class PostgisWriteService implements StorageWriteService {

    private final Logger log;
    private final PostgisSupport postgisSupport;

    private final AgroalDataSource datasource;

    public PostgisWriteService(Logger log, PostgisSupport postgisSupport, AgroalDataSource datasource) {
        this.log = log;
        this.postgisSupport = postgisSupport;
        this.datasource = datasource;
    }

    @Override
    public List<InsertionError> add(Collection<Item> items) {
        List<InsertionError> errors = new ArrayList<>();
        try (var conn = datasource.getConnection()) {
            for (var itemsByClass : items.stream().collect(groupingBy(item -> item.getoClass().getId())).values()) {
                errors.addAll(insertItems(conn, itemsByClass));
            }
        } catch (SQLException e) {
            log.error("Unable to insert items", e);
            return List.of(new InsertionError(null, e.getMessage()));
        }
        return errors;
    }

    private List<InsertionError> insertItems(Connection conn, List<Item> items) throws SQLException {
        var oClass = items.get(0).getoClass();

        String sql = buildInsertSql(oClass);
        log.debugf("SQL : \n%s", sql);
        try (var statement = conn.prepareStatement(sql)) {
            for (Item item : items) {
                int parameterIndex = 0;
                statement.setString(++parameterIndex, item.getId().getId());
                statement.setObject(++parameterIndex, item.getId().getDatasetVersionId());
                List<InsertionError> itemErrors = new ArrayList<>();
                for (var attributeDef : getSortedAttributes(oClass)) { // Use same order as sql
                    itemErrors.addAll(setStatementValues(statement, item, ++parameterIndex, attributeDef));
                }

                if (!itemErrors.isEmpty()) {
                    return itemErrors;
                } else {
                    try {
                        statement.addBatch();
                    } catch (SQLException e) {
                        log.error("Error while adding item %s to batch".formatted(item.getIdAsString()), e);
                        return List.of(new InsertionError(item.getIdAsString(), e.getMessage()));
                    }
                }
            }
            statement.executeBatch();
        }
        return Collections.emptyList();
    }

    private List<InsertionError> setStatementValues(PreparedStatement statement, Item item, int parameterIndex,
            AttributeDefDetailsDto attributeDef) {
        var attribute = item.getAttributeSimple(attributeDef.technicalName);
        Object value = attribute.readValueEvenIfNotVisible();

        try {
            statement.setObject(parameterIndex, preparePGValue(value, attributeDef));
        } catch (SQLException e) {
            log.error("Error while setting parameters for item %s".formatted(item.getIdAsString()), e);
            return List.of(new InsertionError(item.getIdAsString(), e.getMessage()));
        }
        return Collections.emptyList();
    }

    private Object preparePGValue(Object value, AttributeDefDetailsDto attributeDef) {
        return switch (value) {
            case null -> null;
            case GeoHolder geo -> geo.getAsPgObject(attributeDef.field.crs);
            case Instant instant -> Timestamp.from(instant);
            default -> value;
        };
    }

    private List<AttributeDefDetailsDto> getSortedAttributes(OClassDetailsDto oClass) {
        var sortedAttributes = oClass.getAttributes();
        sortedAttributes.sort(Comparator.comparing(oc -> oc.name));
        return sortedAttributes;
    }

    private String buildInsertSql(OClassDetailsDto oClass) {

        //  build column lists => "station_id, totalSpace, freeSpace"
        var columnList = getSortedAttributes(oClass).stream() //  Parameters are filled by position, we must ensure consistency in attributes order
                .map(postgisSupport::getColumnName).toList();
        var joinedAttributes = String.join(", ", columnList);

        // Build parameters list => "?, ?, ?"
        var attributesParameters = oClass.getAttributes().stream()
                .map(attr -> "?")
                .collect(Collectors.joining(", ")); // => " ?,?,?,?"

        return new StringBuilder("insert into ")
                .append(postgisSupport.getTableName(oClass))
                .append(" ( \n")
                .append(PostgisSupport.COLUMN_NAME_ID).append(", ")
                .append(PostgisSupport.COLUMN_NAME_DATASET_VERSION).append(", ")
                .append(joinedAttributes)
                .append("\n) values (\n")
                .append("?, ")
                .append("?, ")
                .append(attributesParameters)
                .append(" \n)")
                .append(" ON CONFLICT (")
                .append(PostgisSupport.COLUMN_NAME_ID)
                .append(") \n")
                .append(" DO UPDATE SET ")
                .append(buildOnConflictSyntax(columnList))
                .toString();

    }

    private String buildOnConflictSyntax(List<String> attributes) {
        return attributes.stream().map(attribute -> "%s = excluded.%s".formatted(attribute, attribute))
                .collect(Collectors.joining(", "));
    }
}
