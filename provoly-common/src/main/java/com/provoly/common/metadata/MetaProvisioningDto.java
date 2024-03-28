package com.provoly.common.metadata;

import java.util.UUID;

public record MetaProvisioningDto(UUID id, String name, UUID metadata, UUID userProfile) {

}