package com.provoly.transfo;

import jakarta.inject.Inject;

import com.provoly.clients.ExecService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.transfo.TransfoDetailsDto;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class LastExecutionMapper {
    @Inject
    @RestClient
    ExecService execService;

    @Inject
    Logger log;

    @AfterMapping
    public void setLastJobExecution(@MappingTarget TransfoDetailsDto transfoDetailsDto) {
        JobExecutionDetailsDto jobExecutionDetailsDto;
        if (transfoDetailsDto.getJobInstanceId() == null) {
            log.infof("No job instance found");
        } else {
            try {
                jobExecutionDetailsDto = execService.getLastJobExecution(transfoDetailsDto.getJobInstanceId());
                transfoDetailsDto.setLastJobExecution(jobExecutionDetailsDto);
            } catch (BusinessException exception) {
                if (exception.getCode() == ErrorCode.NOT_FOUND) {
                    log.infof("No job execution found");
                } else {
                    throw exception;
                }
            }
        }
    }
}
