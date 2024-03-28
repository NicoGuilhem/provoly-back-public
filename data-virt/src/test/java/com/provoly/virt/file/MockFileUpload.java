package com.provoly.virt.file;

import java.nio.file.Path;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public class MockFileUpload implements FileUpload {

    public MockFileUpload(String fileName, String filePath, MediaType mediaType) {

        this.fileName = fileName;
        this.filePath = filePath;
        this.mediaType = mediaType;
    }

    private String fileName;

    private String filePath;

    private MediaType mediaType;

    @Override
    public String name() {
        return "name";
    }

    @Override
    public Path filePath() {
        return Path.of(filePath);
    }

    @Override
    public String fileName() {
        return fileName;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public String contentType() {
        return mediaType.toString();
    }

    public MediaType mediaType() {
        return mediaType;
    }

    @Override
    public String charSet() {
        return null;
    }

    @Override
    public Path uploadedFile() {
        return FileUpload.super.uploadedFile();
    }
}
