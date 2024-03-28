package com.provoly.exec.model;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.exec.EntityIdService;

import org.mapstruct.TargetType;

@ApplicationScoped
public class EntityLoader {

    @Inject
    EntityIdService entityService;

    public <T extends EntityId> T resolve(UUID id, @TargetType Class<T> entityClass) {

        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "mandatory id is null when loading a " + entityClass.getSimpleName());
        }
        return entityService.getById(id, entityClass);
    }

}
