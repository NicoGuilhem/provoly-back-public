package com.provoly.ref.user.metadata;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
public class UserProfileService {
    private EntityIdRepository entityIdRepository;

    UserProfileService(EntityIdRepository entityIdRepository) {
        this.entityIdRepository = entityIdRepository;
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

        entityIdRepository.saveEntity(metadata);
    }

    @Transactional
    public UserProfile getById(UUID id) {
        return entityIdRepository.getById(id, UserProfile.class);
    }

    public List<UserProfile> getAll() {
        return entityIdRepository.getAll(UserProfile.class);
    }

    public void removeIfExists(UUID id) {
        entityIdRepository.removeIfExists(id, UserProfile.class);

    }
}
