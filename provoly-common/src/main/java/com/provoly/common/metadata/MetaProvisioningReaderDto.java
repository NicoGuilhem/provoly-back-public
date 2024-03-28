package com.provoly.common.metadata;

import java.util.UUID;

public record MetaProvisioningReaderDto(UUID id, String name, MetadataDefDto metadata, UserProfileDto userProfile) {
}
