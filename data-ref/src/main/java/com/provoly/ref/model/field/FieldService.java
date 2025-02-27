package com.provoly.ref.model.field;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.field.*;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.model.*;

@ApplicationScoped
public class FieldService {
    private FieldMapper modelMapper;
    private EntityIdRepository entityIdRepository;
    private RefEventService refEventService;
    private AssociationService associationService;
    private FieldRepository fieldRepository;

    public FieldService(FieldMapper modelMapper, EntityIdRepository entityIdRepository,
            RefEventService refEventService, AssociationService associationService,
            FieldRepository fieldRepository) {
        this.modelMapper = modelMapper;
        this.entityIdRepository = entityIdRepository;
        this.refEventService = refEventService;
        this.associationService = associationService;
        this.fieldRepository = fieldRepository;
    }

    public void addField(FieldDto fieldDto) {
        if (fieldRepository.fieldExists(fieldDto.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Field %s already exists.".formatted(fieldDto.getId()));
        }

        fieldDto.checkField();
        Field entity = modelMapper.toModel(fieldDto);
        entityIdRepository.saveEntity(entity);
        FieldDto fieldUpdated = modelMapper.toDto(entity); // Ugly hack : at least to set the slug
        refEventService.fieldAdded(fieldUpdated);
    }

    public void deleteFieldById(UUID id) {
        AssociationsDto associations = associationService.getFieldAssociations(id);

        if (!associations.associations().isEmpty() || associations.usedElsewhere()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "The field %s is used by one or more attributes, remove them to delete the field".formatted(id));
        }

        entityIdRepository.removeEntity(id, Field.class);
    }

    public Collection<Field> getFieldForClass(UUID id) {
        entityIdRepository.checkEntityExists(id, OClass.class);
        return fieldRepository.getFieldForClass(id);
    }

    public Field getFieldById(UUID id) {
        return entityIdRepository.getById(id, Field.class);
    }

    public List<Field> getAllFields() {
        return entityIdRepository.getAll(Field.class);
    }

    public void updateField(UUID id, FieldDto fieldDto) {
        fieldDto.checkField();
        Field field = entityIdRepository.getById(id, Field.class);
        checksUpdate(fieldDto, field);
        entityIdRepository.saveEntity(modelMapper.toModel(fieldDto));
    }

    private void checksUpdate(FieldDto fieldDto, Field field) {
        if (field.getType() != fieldDto.getType()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "You're not allowed to change type %s to %s".formatted(field.getType(), fieldDto.getType()));
        }
    }

}
