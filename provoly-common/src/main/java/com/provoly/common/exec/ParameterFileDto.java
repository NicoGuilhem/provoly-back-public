package com.provoly.common.exec;

public class ParameterFileDto extends ParameterDto {

    private final String filename;

    public ParameterFileDto(String name, String filename) {
        super(name);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

}
