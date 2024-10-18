package com.provoly.virt.imports.indexers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.provoly.virt.imports.model.ItemRecord;

import org.junit.jupiter.api.Test;

public class CsvWalkerTest {

    @Test
    void test_read_csv_file_ok() throws IOException {

        ItemRecord firstItem;
        ItemRecord lastItem = null;
        int count = 1;

        try (FileInputStream in = new FileInputStream(Path.of("src/test/resources/importOk.csv").toFile())) {
            CsvWalker csvWalker = new CsvWalker(in, List.of("StringField", "IntegerField", "InstantField", "GeoField"));
            firstItem = csvWalker.next();
            while (csvWalker.hasNext()) {
                lastItem = csvWalker.next();
                count++;
            }
        }

        assertThat(count).isEqualTo(14);
        assertThat(firstItem).isNotNull();
        assertThat(firstItem.recordId()).isEqualTo("2");
        assertThat(firstItem.values())
                .hasSize(4)
                .containsEntry("StringField", "toto")
                .containsEntry("IntegerField", "100")
                .containsEntry("InstantField", "1977-04-22T06:00:00Z")
                .containsEntry("GeoField", "{\"type\": \"MultiLineString\",\"coordinates\": [[[1.0, 1.0], [1.0, 2.0]]]}");

        assertThat(lastItem).isNotNull();
        assertThat(lastItem.recordId()).isEqualTo("15");
        assertThat(lastItem.values())
                .hasSize(4)
                .containsEntry("StringField", "toto")
                .containsEntry("IntegerField", "100")
                .containsEntry("InstantField", "1977-04-22T06:00:00Z")
                .containsEntry("GeoField", "{\"type\": \"MultiLineString\",\"coordinates\": [[[1.0, 1.0], [1.0, 2.0]]]}");
    }
}
