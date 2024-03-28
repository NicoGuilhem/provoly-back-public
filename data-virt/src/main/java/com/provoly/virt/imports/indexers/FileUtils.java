package com.provoly.virt.imports.indexers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import com.provoly.common.error.ErrorCode;
import com.provoly.virt.imports.model.ImportException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jboss.logging.Logger;

public class FileUtils {

    private FileUtils() {
    }

    private static final Logger LOG = Logger.getLogger(FileUtils.class);

    public static void extractZip(InputStream is, File destDir) throws ImportException {
        LOG.infof("unzipping input to %s", destDir);
        ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(is);
        ArchiveEntry archiveEntry;
        try {
            while ((archiveEntry = zipArchiveInputStream.getNextEntry()) != null) {
                LOG.infof("unzipping file %s to %s", archiveEntry.getName(), destDir);
                if (archiveEntry.isDirectory()) {
                    continue;
                }
                var path = Paths.get(archiveEntry.getName()).getFileName().toString();
                var file = new File(destDir, path);
                try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
                    IOUtils.copy(zipArchiveInputStream, outputStream);
                }
            }
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL, "Error while extracting zip : %s".formatted(e), e);
        }
    }

    public static void delete(File file) throws ImportException {
        LOG.infof("Deleting file: %s", file);
        if (file.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach(FileUtils::delete);
        }
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new ImportException(ErrorCode.TECHNICAL, "Could not delete file: %s : %s".formatted(file, e.getMessage()));
        }
    }
}
