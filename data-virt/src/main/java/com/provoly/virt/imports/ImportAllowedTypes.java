package com.provoly.virt.imports;

import java.util.Arrays;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import com.provoly.virt.entity.FileType;

public enum ImportAllowedTypes {
    CSV(FileType.CSV_TYPE),
    SHP(FileType.SHP_TYPE);

    private final FileType fileType;

    ImportAllowedTypes(FileType fileType) {
        this.fileType = fileType;
    }

    public static Optional<ImportAllowedTypes> findByType(String type) {
        return Arrays.stream(values())
                .filter(allowedType -> allowedType.fileType.equals(FileType.valueOf(type)))
                .findFirst();
    }

    public static Optional<ImportAllowedTypes> findByType(MediaType type) {
        return Arrays.stream(values())
                .filter(allowedType -> allowedType.fileType.equals(type))
                .findFirst();
    }
}
