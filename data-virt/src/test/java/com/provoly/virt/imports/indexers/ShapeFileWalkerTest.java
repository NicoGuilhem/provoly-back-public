package com.provoly.virt.imports.indexers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.provoly.virt.imports.model.ItemRecord;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

public class ShapeFileWalkerTest {

    private Logger logger = Logger.getLogger(ShapeFileWalkerTest.class);

    @Test
    void test_read_shape_file_ok() throws IOException {

        ItemRecord firstItem;
        ItemRecord lastItem = null;
        int count = 1;

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("valid_shapefile.zip")) {
            ShapeFileWalker shpWalker = new ShapeFileWalker(in, List.of("the_geom", "categorie", "Date", "int_long", "float"),
                    logger);
            firstItem = shpWalker.next();
            while (shpWalker.hasNext()) {
                lastItem = shpWalker.next();
                count++;
            }
        }

        assertThat(count).isEqualTo(26);
        assertThat(firstItem).isNotNull();
        assertThat(firstItem.recordId()).isEqualTo("namae_wa_v2.1");
        assertThat(firstItem.values())
                .hasSize(5)
                .containsEntry("categorie", "PME")
                .containsEntry("int_long", Long.parseLong("3123456798132456"))
                .containsEntry("float", Double.parseDouble("8.002548232456482E9"))
                .containsEntry("Date",
                        Date.from(LocalDateTime.parse("2024-05-07T00:00:00").atZone(ZoneId.systemDefault()).toInstant()))
                .containsKey("the_geom")
                .extractingByKey("the_geom")
                .asString()
                .isEqualTo(
                        "{\"type\":\"Point\",\"coordinates\":[6.58872,43.38589],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:0\"}}}");

        assertThat(lastItem).isNotNull();
        assertThat(lastItem.recordId()).isEqualTo("namae_wa_v2.26");
        assertThat(lastItem.values())
                .hasSize(5)
                .containsEntry("categorie", "Trait")
                .containsEntry("int_long", Long.parseLong("3123456798132456"))
                .containsEntry("float", Double.parseDouble("8.002548232456482E9"))
                .containsEntry("Date",
                        Date.from(LocalDateTime.parse("2024-02-09T00:00:00").atZone(ZoneId.systemDefault()).toInstant()))
                .containsKey("the_geom")
                .extractingByKey("the_geom")
                .asString()
                .isEqualTo(
                        "{\"type\":\"Point\",\"coordinates\":[2.841818943794274,45.9670211036167],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:0\"}}}");
    }
}
