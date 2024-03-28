package com.provoly.ref.widget.dto;

import java.util.Collection;
import java.util.UUID;

import com.provoly.common.search.VisibilityDto;

public class WidgetDto {
    public UUID id;
    public String name;
    public String description;
    public String image;
    public String content;
    public VisibilityDto visibility;
    public Collection<UUID> datasource;
    public boolean cover;
}