package com.provoly.exec.model;

import java.util.Collection;

import com.provoly.common.exec.*;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.SubclassExhaustiveStrategy;

@Mapper(componentModel = "jakarta", subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, // Needed for mapping abstract class
        uses = { EntityLoader.class }

)
public interface JobMapper {

    JobModel toEntity(JobModelDto dto);

    JobInstance toEntity(JobInstanceDto dto);

    JobExecution toEntity(JobExecutionDto dto);

    JobModelDto toDto(JobModel entity);

    JobInstanceDto toDto(JobInstance entity);

    JobInstanceDetailsDto toDetailsDto(JobInstance entity);

    JobExecutionDetailsDto toDetailsDto(JobExecution entity);

    Collection<JobModelDto> toCollectionDto(Collection<JobModel> model);

    Collection<JobInstanceDetailsDto> toJobInstanceDetailsDtoCollection(Collection<JobInstance> entity);

    Collection<JobExecutionDetailsDto> jobExecutionToDto(Collection<JobExecution> jobExecution);

    void update(JobModelDto dto, @MappingTarget JobModel model);

    void update(JobInstanceDto dto, @MappingTarget JobInstance model);

    default ParameterDto toDto(Parameter entity) {
        return new ParameterFileDto(entity.getName(), entity.getFilename());
    }

    default Parameter toEntity(ParameterDto dto) {
        // TODO : Switch to a pattern matching
        if (dto instanceof ParameterFileDto) {
            var parameterFileDto = (ParameterFileDto) dto;
            return new Parameter(parameterFileDto.getName(), parameterFileDto.getFilename());
        }
        throw new IllegalArgumentException("ParameterDto type is unsupported " + dto.getClass());
    }
}
