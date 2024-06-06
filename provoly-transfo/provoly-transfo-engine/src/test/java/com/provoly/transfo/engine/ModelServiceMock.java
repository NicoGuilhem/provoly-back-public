package com.provoly.transfo.engine;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.clients.ModelService;
import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.*;
import com.provoly.test.DatasetFactory;

import io.quarkus.test.Mock;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Mock
@RestClient
public class ModelServiceMock implements ModelService {
    private final FieldDto fieldName;
    private final FieldDto fieldSpace;

    public ModelServiceMock() {
        this.fieldName = createField("name", Type.KEYWORD);
        this.fieldSpace = createField("space", Type.INTEGER);
    }

    @Override
    public Collection<FieldDto> getFieldsForClass(UUID id) {
        if (id.equals(DatasetFactory.BIKE_STATION_OCLASS_ID)) {
            return List.of(fieldName, fieldSpace);
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "Unknown class " + id);
    }

    @Override
    public FieldDto getFieldById(UUID id) {
        return null;
    }

    @Override
    public OClassReadDto get(UUID id) {
        if (id.equals(DatasetFactory.BIKE_STATION_OCLASS_ID)) {
            var attrName = createAttribute("name", fieldName);
            var attrTotalSpace = createAttribute("totalSpace", fieldSpace);
            var attrFreeSpace = createAttribute("freeSpace", fieldSpace);

            return createClass("Bike station", attrName, attrFreeSpace, attrTotalSpace);
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "Unknown class " + id);
    }

    @Override
    public OClassDetailsDto getDetails(UUID id) {
        if (id.equals(DatasetFactory.BIKE_STATION_OCLASS_ID)) {
            var attrName = createAttributeDetails("name", fieldName);
            var attrTotalSpace = createAttributeDetails("totalSpace", fieldSpace);
            var attrFreeSpace = createAttributeDetails("freeSpace", fieldSpace);

            return createClassDetails("Bike station", attrName, attrFreeSpace, attrTotalSpace);

        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "Unknown class " + id);
    }

    @Override
    public void addClass(OClassWriteDto oclassDto) {

    }

    @Override
    public Collection<OClassReadDto> getAllClasses() {
        return null;
    }

    @Override
    public void deleteClass(UUID id) {

    }

    @Override
    public void addFields(Collection<FieldDto> fieldDtoCollection) {

    }

    private FieldDto createField(String name, Type type) {
        var fieldDto = new FieldDto();
        fieldDto.id = UUID.randomUUID();
        fieldDto.name = name + "-" + fieldDto.id;
        fieldDto.type = type.name();
        return fieldDto;
    }

    private AttributeDefDto createAttribute(String name, FieldDto field) {
        AttributeDefDto attr = new AttributeDefDto();
        attr.setId(UUID.randomUUID());
        attr.setName(name);
        attr.setField(field.id);
        return attr;
    }

    private AttributeDefDetailsDto createAttributeDetails(String name, FieldDto field) {
        AttributeDefDetailsDto attr = new AttributeDefDetailsDto();
        attr.setId(UUID.randomUUID());
        attr.setName(name);
        attr.setField(field);
        return attr;
    }

    private OClassReadDto createClass(String name, AttributeDefDto... attributes) {
        UUID oclassId = UUID.randomUUID();
        return new OClassReadDto(oclassId, name + "-" + oclassId, List.of(attributes), Storage.ELASTIC);
    }

    private OClassDetailsDto createClassDetails(String name, AttributeDefDetailsDto... attributes) {
        var slug = "slug";
        return new OClassDetailsDto(UUID.randomUUID(), slug, "{}-{}".formatted(name, slug), "", List.of(attributes),
                Storage.ELASTIC, List.of());
    }
}
