package com.provoly.virt.file;

import java.io.InputStream;

public record MinIoFileUpload(InputStream inputStream, String fileName, String mediaType, String bucket, String type) {
}
