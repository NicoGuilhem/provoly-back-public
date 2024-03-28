package com.provoly.common.datasource;

import java.util.UUID;

public record DataSourceDetailsDto(UUID id, DataSourceType type, UUID oClass) {

}
