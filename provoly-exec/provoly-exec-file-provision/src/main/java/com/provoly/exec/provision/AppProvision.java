package com.provoly.exec.provision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.clients.ProvolyExecEngineService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.Constants;
import com.provoly.common.exec.JobInstanceDetailsDto;
import com.provoly.common.exec.ParameterFileDto;
import com.provoly.common.exec.ParameterValueDto;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@QuarkusMain
public class AppProvision implements QuarkusApplication {

    @Inject
    Logger log;

    @RestClient
    ProvolyExecEngineService execEngineService;

    @ConfigProperty(name = Constants.ENV_NAME_JOB_EXECUTION_ID)
    UUID jobExecutionId;

    @ConfigProperty(name = "provoly.exec.file-base-path", defaultValue = Constants.STR_DATA_BASE_PATH)
    Path basePath;

    // TODO : Only parameter of type file should be written
    // TODO : Check main container don't start if return is not 0,
    // TODO : check return is not 0 in case exception or container not start e.g. if jobInstanceId is not valid

    @Override
    public int run(String... args) {
        log.infof("Starting file provisioning for jobExecutionId : %s", jobExecutionId);
        log.info("Check base path exists");
        if (!Files.isDirectory(basePath)) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Base Path not exists : " + basePath.toAbsolutePath().normalize());
        }
        if (!Files.isWritable(basePath)) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Base Path is not writable : " + basePath.normalize());
        }
        log.info("Get execution details from provoly-exec-engine");
        var jobExecutionDto = execEngineService.getJobExecution(jobExecutionId);
        JobInstanceDetailsDto jobInstance = jobExecutionDto.getInstance();
        jobInstance.getParametersValue().forEach(v -> writeFile(jobInstance, v));
        return 0;
    }

    private void writeFile(JobInstanceDetailsDto instance, ParameterValueDto parameterValue) {
        try {
            ParameterFileDto parameter = instance.getParameter(parameterValue);
            Path filePath = basePath.resolve(parameter.getFilename());
            if (Files.exists(filePath)) {
                throw new BusinessException(ErrorCode.FILE_ALREADY_EXISTS, filePath.toAbsolutePath().toString());
            }
            Files.write(filePath, parameterValue.getValue().getBytes());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
