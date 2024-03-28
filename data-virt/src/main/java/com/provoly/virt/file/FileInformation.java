package com.provoly.virt.file;

import java.io.InputStream;

import io.minio.StatObjectResponse;

public record FileInformation(String id, StatObjectResponse objectStats, InputStream is, long start, long end) {
}
