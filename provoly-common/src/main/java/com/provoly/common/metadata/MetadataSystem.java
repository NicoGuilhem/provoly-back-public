package com.provoly.common.metadata;

import java.util.UUID;

import com.provoly.common.VariableType;

public enum MetadataSystem {

    CLASS("9d9aba2c-fc3a-44d7-a508-287ad2d8cbb0", "d61cebf7ca__class", "_class", VariableType.UUID),
    DATASET_VERSION("99f6c00b-de55-4200-8427-6694530915f7", "99f6c00b_dataset_version_id", "_dataset_version_id",
            VariableType.UUID),
    DATASET("99c8912d-cc1d-440c-8b68-0c28d50bda19", "99c8912d__dataset_id", "_dataset_id",
            VariableType.UUID),
    INSERTION_DATE("bc85f134-8a8c-44e5-b750-06f7b3b27be1", "bc85f134__insertion_date", "_insertion_date", VariableType.DATE),
    ID("3223ff86-f1e4-42d3-b467-961d72cb09d7", "3223ff86__item_id", "_item_id", VariableType.STRING),
    GEO_NAMESPACE("dedfb486-4387-4906-8724-adf7bc637a33", "dedfb486__geoNamespace", "_geoNamespace", VariableType.STRING),
    GEO_KEY("146536da-01da-4538-990a-78a11c4696d8", "146536da__geoKey", "_geoKey", VariableType.STRING),
    THEME("f34754e6-be23-43c0-bb78-3d351e26c033", "f34754e6__theme", "_theme", VariableType.LIST),
    ASSET_MODEL("b6f5502f-4412-4858-b181-ddf6c100820f", "b6f5502f__assetModel", "_assetModel", VariableType.STRING),
    MEASURE_NAME("2e734460-c1c1-4785-9902-aebb772e58f0", "2e734460__measureName", "_measureName", VariableType.STRING);

    private final MetadataDefDto metadata;

    MetadataSystem(String id, String slug, String name, VariableType type) {
        this.metadata = new MetadataDefDto();
        metadata.id = UUID.fromString(id);
        metadata.name = name;
        metadata.type = type;
        metadata.slug = slug;
    }

    public UUID getId() {
        return metadata.id;
    }

    public String getName() {
        return metadata.name;
    }

    public MetadataDefDto getMetadata() {
        return metadata;
    }

    public String getType() {
        return metadata.type.name();
    }

    public boolean is(UUID otherId) {
        return metadata.id.equals(otherId);
    }
}