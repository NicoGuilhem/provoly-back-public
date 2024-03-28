package com.provoly.virt.imports.indexers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.provoly.common.error.ErrorCode;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.imports.model.ImportException;
import com.provoly.virt.imports.model.ItemRecord;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureCollection;
import org.jboss.logging.Logger;
import org.locationtech.jts.geom.Geometry;

public class ShapeFileWalker implements FileWalker {
    private Logger log;
    private List<String> attributeNames;
    private SimpleFeatureIterator featureIterator;
    private List<String> featureAttributes;
    private ShapefileDataStore shpDataStore;
    private File extractDirectory;

    public ShapeFileWalker(InputStream is, List<String> attributeNames, Logger logger)
            throws ImportException {
        this.log = logger;
        log.info("Initialize ShapeFile parser and iterator");
        this.attributeNames = attributeNames;
        log.info("Load inputStream into disk");
        try {
            extractDirectory = Files.createTempDirectory("extractDirectory").toFile();
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL,
                    "Error while trying to create temporary directory: %s".formatted(e.getMessage()));
        }

        FileUtils.extractZip(is, extractDirectory);
        File shpFile;

        try (Stream<Path> shpFiles = Files.find(extractDirectory.toPath(), Integer.MAX_VALUE, this::isaShapeFile)) {
            shpFile = shpFiles.findFirst().orElseThrow(() -> new ImportException(
                    ErrorCode.BAD_REQUEST, "no shapefile found")).toFile();
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL,
                    "Error while trying to find shapefile in extracted directory: %s".formatted(e.getMessage()));
        }

        logger.info("Creating shapefile DataStore");
        ContentFeatureCollection featureCollection;
        SimpleFeatureType schema;
        try {
            shpDataStore = new ShapefileDataStore(shpFile.toURI().toURL());
            shpDataStore.setTryCPGFile(true);
            schema = shpDataStore.getSchema();
            featureCollection = shpDataStore.getFeatureSource().getFeatures();
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL,
                    "Error while initializing ShapefileDatastore: %s".formatted(e.getMessage()));
        }

        featureAttributes = schema.getAttributeDescriptors().stream()
                .map(att -> att.getName().toString())
                .toList();
        this.attributeNames = featureAttributes
                .stream()
                .filter(attributeNames::contains)
                .toList();

        this.featureIterator = featureCollection.features();
    }

    private boolean isaShapeFile(Path filePath, BasicFileAttributes fileAttribute) {
        return filePath.toString().endsWith(".shp") && fileAttribute.isRegularFile();
    }

    public ItemRecord next() {
        SimpleFeature feature = featureIterator.next();
        Map<String, Object> values = new HashMap<>();
        attributeNames.forEach(attrName -> values.put(attrName, extractValue(feature, attrName)));
        return new ItemRecord(feature.getID(), values);
    }

    private Object extractValue(SimpleFeature feature, String attrName) {
        Object value = feature.getAttribute(attrName);
        if (value instanceof Geometry geometry) {
            value = new GeoHolder(geometry);
        }
        return value;
    }

    public boolean hasNext() {
        return featureIterator.hasNext();
    }

    public void close() {
        log.debug("Closing resources");
        featureIterator.close();
        shpDataStore.dispose();
        FileUtils.delete(extractDirectory);
    }

    public List<String> getAttributes() {
        return featureAttributes;
    }
}
