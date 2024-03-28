package com.provoly.exec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.exec.*;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyTestContainers;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
public class JobServiceCrudTest {

    private static final String EXPECTED_IMAGE_NAME = "imageName";
    private static final String FILE_CONTENT = "file_content";

    @Inject
    AuthService authService;

    @Inject
    JobInstanceController jobInstanceController;

    @Inject
    JobModelController jobModelController;

    @Test
    public void createAndRetrieveJobInstanceWithFileContent() {
        authService.authenticate();

        final String PARAMETER_NAME = "name";
        String EXPECTED_FILENAME = "transfo.json";
        Set<ParameterDto> parameters = Set.of(new ParameterFileDto(PARAMETER_NAME, EXPECTED_FILENAME));
        var jobModelDto = new JobModelDto(UUID.randomUUID(), EXPECTED_IMAGE_NAME, parameters);
        jobModelController.save(jobModelDto);

        var expectedJobInstance = new JobInstanceBuilder()
                .withParameterValue(PARAMETER_NAME, FILE_CONTENT)
                .build(jobModelDto.getId());

        jobInstanceController.save(expectedJobInstance);
        var result = jobInstanceController.get(expectedJobInstance.getId());

        // Check model parameter path is retrieved
        assertThat(result.getModel().getParameters())
                .hasSize(1)
                .first()
                .asInstanceOf(type(ParameterFileDto.class))
                .extracting(ParameterFileDto::getFilename)
                .isEqualTo(EXPECTED_FILENAME);

        // Check parameter value is retrieved
        assertThat(result.getParametersValue())
                .hasSize(1)
                .first()
                .extracting(ParameterValueDto::getValue)
                .isEqualTo(FILE_CONTENT);

    }

    // TODO : Test model is not null <- actually it is =(
}
