package com.provoly.ref.model;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.category.Category;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdService;

import org.mapstruct.TargetType;

@ApplicationScoped
public class EntityLoader {

    @PersistenceContext
    EntityManager em;

    @Inject
    Instance<EntityIdService> entityService;

    static final UUID DEFAULT_CATEGORY_ID = UUID.fromString("cf666d66-838f-4d92-a4d2-a315df21fac9");

    public <T extends EntityId> T resolve(UUID id, @TargetType Class<T> entityClass) {
        if (entityClass.equals(Category.class) && id == null) {
            id = DEFAULT_CATEGORY_ID;
        }

        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "mandatory id is null when loading a " + entityClass.getSimpleName());
        }
        return entityService.stream().findFirst().get().getById(id, entityClass);
    }

    public UUID toReference(EntityId entity) {
        return entity.getId();
    }
}
