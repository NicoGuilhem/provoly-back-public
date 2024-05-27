package com.provoly.ref.widget.dto;

import java.util.Collection;
import java.util.UUID;

public record WidgetWriteDto(UUID id, String name, String description, String image, String content,
        Collection<UUID> datasource, boolean cover, Collection<String> groups) {
}