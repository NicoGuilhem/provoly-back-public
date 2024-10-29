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
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.field.FieldGeoDto;
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
    public List<InsertionError> addOrUpdate(Collection<Item> items, ItemUpdateMode updateMode) {
        List<InsertionError> errors = new ArrayList<>();
        try (var conn = datasource.getConnection()) {
            for (var itemsByClass : items.stream().collect(groupingBy(item -> item.getoClass().getId())).values()) {
                errors.addAll(insertOrUpdateItems(conn, itemsByClass, updateMode));
            }
        } catch (SQLException e) {
            log.error("Unable to insert or update items", e);
            return List.of(new InsertionError(null, e.getMessage()));
        } catch (BusinessException e) {
            log.error("Unable to insert or update items", e);
            return List.of(new InsertionError(null, e));
        }
        return errors;
    }

    private List<InsertionError> insertOrUpdateItems(Connection conn, List<Item> items, ItemUpdateMode updateMode)
            throws SQLException {

        String sql = buildInsertOrUpdateSql(items, updateMode);
        log.debugf("SQL : \n%s", sql);
        try (var statement = conn.prepareStatement(sql)) {
            for (Item item : items) {
                int parameterIndex = 0;
                statement.setString(++parameterIndex, item.getId().getId());
                statement.setObject(++parameterIndex, item.getId().getDatasetVersionId());
                List<InsertionError> itemErrors = new ArrayList<>();
                for (var attributeDef : getSortedAttributes(items, updateMode)) { // Use same order as sql
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
        var attribute = item.getAttributeSimple(attributeDef.getTechnicalName());
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
            case GeoHolder geo -> geo.getAsPgObject(((FieldGeoDto) attributeDef.getField()).getCrs());
            case Instant instant -> Timestamp.from(instant);
            default -> value;
        };
    }

    private List<AttributeDefDetailsDto> getSortedAttributes(List<Item> items, ItemUpdateMode updateMode) {
        var sortedAttributes = getAttributeListToInsertOrUpdate(items, updateMode);
        sortedAttributes.sort(Comparator.comparing(AttributeDefDetailsDto::getName));
        return sortedAttributes;
    }

    /**
     * Get the list of attributes to insert or update depending on the update mode<br>
     * <ul>
     * <li>in REPLACE mode : we must insert or update all attributes of the class</li>
     * <li>in MERGE mode, we must insert or update only the attributes of the first item
     * which must be the same for all items</li>
     * </ul>
     *
     * @param items the items to insert or update
     * @param updateMode the mode to use
     * @return the list of attributes to insert or update
     */
    private List<AttributeDefDetailsDto> getAttributeListToInsertOrUpdate(List<Item> items, ItemUpdateMode updateMode) {
        var firstItem = items.getFirst();
        if (updateMode == ItemUpdateMode.REPLACE) {
            return firstItem.getoClass().getAttributes();
        } else {
            // when merging, we must first ensure that all items have the sames attributes
            // as only the first item is used to build the SQL UPDATE query
            var firstItemAttributes = String.join(", ", items.stream().findFirst().get().getSortedAttributes());
            if (items.stream().anyMatch(item -> !firstItemAttributes.equals(String.join(", ", item.getSortedAttributes())))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "With postgis storage, using update mode MERGE, all items must have the same attributes.");
            }

            var oClass = firstItem.getoClass();
            return oClass.getAttributes().stream()
                    .filter(classAttr -> firstItem.getAttributes().containsKey(classAttr.getTechnicalName()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private String buildInsertOrUpdateSql(List<Item> items, ItemUpdateMode updateMode) {

        //  build column lists => "station_id, totalSpace, freeSpace"
        var columnList = getSortedAttributes(items, updateMode).stream() //  Parameters are filled by position, we must ensure consistency in attributes order
                .map(postgisSupport::getColumnName).toList();
        var joinedAttributes = String.join(", ", columnList);

        // Build parameters list => "?, ?, ?"
        var attributesParameters = columnList.stream()
                .map(attr -> "?")
                .collect(Collectors.joining(", ")); // => " ?,?,?,?"

        var oClass = items.getFirst().getoClass();
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
