package com.provoly.virt.storage.postgis;

import static com.provoly.virt.storage.postgis.PostgisSupport.TECHNICAL_COLUMNS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.virt.storage.StorageModelService;
import com.provoly.virt.storage.StorageQualifier;

import io.agroal.api.AgroalDataSource;

import org.jboss.logging.Logger;

/**
 * Create/Update postgis tables for a class
 */
@StorageQualifier(Storage.POSTGIS)
@ApplicationScoped
class PostgisModelService implements StorageModelService {

    private Logger log;
    private PostgisSupport postgisSupport;
    private AgroalDataSource dataSource;

    public PostgisModelService(Logger log, PostgisSupport postgisSupport, AgroalDataSource dataSource) {
        this.log = log;
        this.postgisSupport = postgisSupport;
        this.dataSource = dataSource;
    }

    @Override
    public void createOClass(OClassDetailsDto oClass) {
        try (var conn = dataSource.getConnection();
                var statement = conn.createStatement()) {
            String sql = buildCreateSql(oClass);
            log.debugf("SQL : \n%s", sql);
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Unable to create oClass with Id " + oClass.getId() + "and slug " + oClass.getSlug(), e);
        }
    }

    /**
     * Note: Delete an attribute is not supported by Provoly for now. However, it has been coded here
     *
     * @param oClass
     */
    @Override
    public void updateOClass(OClassDetailsDto oClass) {
        try (var conn = dataSource.getConnection();
                var statement = conn.createStatement()) {

            var attributesByCol = oClass.getAttributes().stream()
                    .collect(Collectors.toMap(postgisSupport::getColumnName, attr -> attr));

            var alterTableActions = new ArrayList<String>();

            String listColumnSql = "select column_name, udt_name from information_schema.columns where table_name = '"
                    + postgisSupport.getTableName(oClass) + "'";
            var rs = statement.executeQuery(listColumnSql);
            while (rs.next()) {
                var columnName = rs.getString("column_name");
                // Detect if we need to drop column;
                if (!TECHNICAL_COLUMNS.contains(columnName) && attributesByCol.remove(columnName) == null) {
                    // Column is no more in class definition
                    log.infof("Will drop column %s", columnName);
                    alterTableActions.add("drop " + columnName);
                }
            }

            // Every column already defined has been removed from attributesByCol
            // Create missing columns
            for (AttributeDefDetailsDto attribute : attributesByCol.values()) {
                log.infof("Will add column for attribute %s", attribute);
                var addColAction = "    add " + buildColumnSql(attribute);
                alterTableActions.add(addColAction);
            }

            if (alterTableActions.isEmpty()) {
                log.infof("No update on %s structure, skipping", oClass.getSlug());
                return;
            }

            var alterTableAtionsString = String.join(",\n", alterTableActions);
            var sql = "alter table " + postgisSupport.getTableName(oClass) + "\n" + alterTableAtionsString;

            log.debugf("SQL : \n%s", sql);
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Unable to update oClass with Id " + oClass.getId() + "and slug " + oClass.getSlug(), e);
        }
    }

    @Override
    public void deleteOClass(OClassDetailsDto oClass) {
        try (var conn = dataSource.getConnection();
                var statement = conn.createStatement()) {

            String sql = buildDropSql(oClass);
            log.debugf("SQL : \n%s", sql);
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to delete oClass " + oClass.getSlug(), e);
        }
    }

    @Override
    public void deleteDatasetVersion(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto) {
        String deleteQuery = """
                    delete from %s
                    where provoly_dataset_version = ?
                """.formatted(postgisSupport.getTableName(oClassDetailsDto));
        try (var conn = dataSource.getConnection()) {
            deleteAssociatedItems(datasetVersionDto, deleteQuery, conn);
        } catch (Exception e) {
            log.error("An error occurred while deleting dataset version %s".formatted(datasetVersionDto.getId()), e);
        }
    }

    private void deleteAssociatedItems(DatasetVersionDto datasetVersionDto, String deleteQuery, Connection conn) {
        try (var ps = conn.prepareStatement(deleteQuery)) {
            log.debug("Query for deleting dataset version %s items : %s".formatted(datasetVersionDto.getId(), deleteQuery));
            ps.setObject(1, datasetVersionDto.getId());
            ps.execute();
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Unable to delete items associated to dataset version %s".formatted(datasetVersionDto.getId()), e);
        }
    }

    private String buildCreateSql(OClassDetailsDto oClass) {
        StringBuilder sql = new StringBuilder("create table " + postgisSupport.getTableName(oClass) + " ( \n");

        var columns = oClass.getAttributes().stream()
                .map(this::buildColumnSql)
                .collect(Collectors.joining(",\n"));

        sql.append(PostgisSupport.COLUMN_NAME_ID + " varchar primary key not null, \n");
        sql.append(PostgisSupport.COLUMN_NAME_DATASET_VERSION + " uuid not null, \n");
        sql.append(columns);
        sql.append("\n)\n");
        return sql.toString();
    }

    private String buildDropSql(OClassDetailsDto oClass) {
        return "drop table " + postgisSupport.getTableName(oClass);
    }

    private String buildColumnSql(AttributeDefDetailsDto attribute) {
        return postgisSupport.getColumnName(attribute) + " " + getPostgisType(attribute);
    }

    private String getPostgisType(AttributeDefDetailsDto attribute) {
        FieldDto field = attribute.getField();
        Type type = field.getType();

        return type.isGeo() ? type.getPostgisType().getGeoType(field.checkAndExtractSRID())
                : type.getPostgisType().getType();
    }

}
