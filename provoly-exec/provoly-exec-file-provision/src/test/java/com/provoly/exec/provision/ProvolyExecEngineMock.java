package com.provoly.exec.provision;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.provoly.clients.ProvolyExecEngineService;
import com.provoly.common.exec.*;

import io.quarkus.test.Mock;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Mock
@RestClient
public class ProvolyExecEngineMock implements ProvolyExecEngineService {

    static final UUID TEST_MODEL_ID = UUID.fromString("423b5c01-1816-41d4-b358-000000000000");
    static final UUID TEST_INSTANCE_ID = UUID.fromString("423b5c01-1816-41d4-b358-000000000010");

    static final UUID TEST_EXECUTION_ID = UUID.fromString("423b5c01-1816-41d4-b358-000000000020");

    static final String TEST_PARAMETER_NAME = "param_file_name";
    static final String TEST_FILE_NAME = "file.transfo";
    static final String TEST_FILE_CONTENT = "The file content";

    @Override
    public JobExecutionDetailsDto getJobExecution(UUID id) {
        if (TEST_EXECUTION_ID.equals(id)) {
            var jobInstance = new JobInstanceDetailsDto(TEST_INSTANCE_ID, buildJobModel(), Collections.emptySet(),
                    Collections.emptySet(), buildParameterValues(), true);
            return new JobExecutionDetailsDto(TEST_EXECUTION_ID, jobInstance, null, null);
        }
        throw new IllegalArgumentException("Unknown id " + id);
    }

    private static Set<ParameterValueDto> buildParameterValues() {
        return Set.of(new ParameterValueDto(TEST_PARAMETER_NAME, TEST_FILE_CONTENT));
    }

    private JobModelDto buildJobModel() {
        var parameterFile = new ParameterFileDto(TEST_PARAMETER_NAME, TEST_FILE_NAME);
        return new JobModelDto(TEST_MODEL_ID, "job image", Set.of(parameterFile));
    }
}
