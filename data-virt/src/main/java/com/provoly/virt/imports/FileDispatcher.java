package com.provoly.virt.imports;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.ErrorCode;
import com.provoly.virt.file.FileInformation;
import com.provoly.virt.imports.indexers.CsvWalker;
import com.provoly.virt.imports.indexers.FileWalker;
import com.provoly.virt.imports.indexers.ShapeFileWalker;
import com.provoly.virt.imports.model.ImportException;

import org.jboss.logging.Logger;

@ApplicationScoped
public class FileDispatcher {

    private final Logger log;

    public FileDispatcher(Logger log) {
        this.log = log;
    }

    public FileWalker dispatch(FileInformation fileInformation, List<String> attributeNames) {
        log.info("Dispatch %s to appropriate indexer".formatted(fileInformation.objectStats().contentType()));
        ImportAllowedTypes allowedType = ImportAllowedTypes.findByType(fileInformation.objectStats().contentType())
                .orElseThrow(() -> new ImportException(ErrorCode.BAD_REQUEST,
                        "Content-type %s not supported".formatted(fileInformation.objectStats().contentType())));
        return switch (allowedType) {
            case SHP -> new ShapeFileWalker(fileInformation.is(), attributeNames, log);
            case CSV -> new CsvWalker(fileInformation.is(), attributeNames);
        };
    }
}
