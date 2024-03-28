package com.provoly.ref.model;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntitySlug;
import com.provoly.ref.entity.SlugifyService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class EntitySlugMapper {
    @Inject
    SlugifyService slugifyService;

    @Inject
    Instance<EntityIdService> entityService;

    @AfterMapping
    public void generateSlug(@MappingTarget EntitySlug entitySlug) {
        EntitySlug existingEntity = entityService.stream().findFirst().get().findById(entitySlug.getId(),
                entitySlug.getClass());
        if (existingEntity == null) {
            String slug = switch (entitySlug) {
                case AttributeDef att -> slugifyService.makeSlug(att.getTechnicalName());
                default -> slugifyService.makeSlug(entitySlug.getName());
            };

            entitySlug.setSlug(slug);

            if (entitySlug instanceof OClass oclass) {
                oclass.getAttributes()
                        .forEach(attributeDef -> attributeDef
                                .setSlug(slugifyService.makeAttributeSlug(attributeDef.getTechnicalName(),
                                        entitySlug.getName())));
            }
        } else {
            entitySlug.setSlug(existingEntity.getSlug());
        }
    }

}
