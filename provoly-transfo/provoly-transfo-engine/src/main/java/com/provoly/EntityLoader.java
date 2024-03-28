package com.provoly;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import org.mapstruct.TargetType;

@ApplicationScoped
public class EntityLoader {

    @Inject
    Instance<EntityIdService> entityService;

    public <T extends EntityId> T resolve(UUID id, @TargetType Class<T> entityClass) {

        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "mandatory id is null when loading a " + entityClass.getSimpleName());
        }
        return entityService.stream().findFirst().get().getById(id, entityClass);
    }

}
