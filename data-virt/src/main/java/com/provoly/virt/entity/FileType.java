package com.provoly.virt.entity;

import java.util.List;

import jakarta.ws.rs.core.MediaType;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

// TODO : validate the file's location in the project's architecture
public class FileType extends MediaType {

    public static final FileType CSV_TYPE = new FileType("text", "csv");
    public static final FileType PNG_TYPE = new FileType("image", "png");
    public static final FileType JPEG_TYPE = new FileType("image", "jpeg");
    public static final FileType SHP_TYPE = new FileType("application", "shp");

    public FileType(String type, String subtype) {
        super(type, subtype);
    }

    public static final List<MediaType> getAuthorizedIconTypes() {
        return List.of(FileType.PNG_TYPE, FileType.JPEG_TYPE);
    }

    public static FileType valueOf(final String type) {
        String[] types = type.split("/");
        if (types.length != 2) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Could not parse media type: %s".formatted(type));
        }

        return new FileType(types[0], types[1]);
    }

}
