package com.provoly.virt.imports.indexers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.error.ErrorCode;
import com.provoly.virt.imports.model.ImportException;
import com.provoly.virt.imports.model.ItemRecord;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;

public class CsvWalker implements FileWalker {
    private static final Logger LOG = Logger.getLogger(CsvWalker.class);
    public static final int IGNORE_HEADER = 1;
    private final Iterator<CSVRecord> recordIterator;

    private final List<String> headers;

    private List<String> attributeNames;

    private Reader reader;

    private File extractDirectory;
    private InputStream localIS;

    public CsvWalker(InputStream is, List<String> attributeNames) {
        LOG.info("Initialize CSV parser and iterator");
        try {
            extractDirectory = Files.createTempDirectory("csvExtractDirectory").toFile();
            Path localFile = Path.of(extractDirectory.getAbsolutePath(), UUID.randomUUID().toString());
            Files.copy(is, localFile, StandardCopyOption.REPLACE_EXISTING);
            localIS = new FileInputStream(localFile.toFile());
            is.close();
            reader = new InputStreamReader(localIS);
            CSVParser records = CSVFormat.EXCEL.builder()
                    .setHeader()
                    .setDelimiter(";")
                    .setRecordSeparator(System.lineSeparator())
                    .build()
                    .parse(reader);
            this.recordIterator = records.stream().iterator();
            this.headers = records.getHeaderNames();
            this.attributeNames = headers
                    .stream()
                    .filter(attributeNames::contains)
                    .toList();
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL,
                    "Error while initializing CSV reader: %s".formatted(e.getMessage()));
        }

    }

    public ItemRecord next() {
        CSVRecord record = recordIterator.next();

        Map<String, Object> map = new HashMap<>();
        attributeNames.forEach(name -> map.put(name, record.get(name)));

        return new ItemRecord(String.valueOf(record.getRecordNumber() + IGNORE_HEADER), map);
    }

    public boolean hasNext() {
        return recordIterator.hasNext();
    }

    public void close() throws IOException {
        localIS.close();
        reader.close();
    }

    public List<String> getAttributes() {
        return headers;
    }

}
