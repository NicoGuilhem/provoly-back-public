package com.provoly.virt.imports;

import java.nio.file.Path;

import com.provoly.virt.entity.FileType;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public record ImportFileParameters(Path file, FileType mediaType, boolean normalizeGeo, Integer chunkSize) {
    public ImportFileParameters(FileUpload file, boolean normalizeGeo, Integer chunkSize) {
        this(file.uploadedFile(), FileType.valueOf(file.contentType()), normalizeGeo, chunkSize);
    }
}
