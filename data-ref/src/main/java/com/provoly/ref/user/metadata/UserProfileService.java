package com.provoly.ref.user.metadata;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
public class UserProfileService {
    private EntityIdService entityIdService;

    UserProfileService(EntityIdService entityIdService) {
        this.entityIdService = entityIdService;
    }

    @Transactional
    public void saveUserProfile(UserProfile metadata) {
        Set<? extends UserProfileAllowedValue> values = metadata.getValues();

        if (!values.isEmpty() && metadata.getType() != VariableType.LIST) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only type LIST accept allowedValues");
        }
        if (metadata.getType() == VariableType.LIST && values.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Metadata type LIST must have allowedValues");
        }

        entityIdService.saveEntity(metadata);
    }

    @Transactional
    public UserProfile getById(UUID id) {
        return entityIdService.getById(id, UserProfile.class);
    }

    public List<UserProfile> getAll() {
        return entityIdService.getAll(UserProfile.class);
    }

    public void removeIfExists(UUID id) {
        entityIdService.removeIfExists(id, UserProfile.class);

    }
}
