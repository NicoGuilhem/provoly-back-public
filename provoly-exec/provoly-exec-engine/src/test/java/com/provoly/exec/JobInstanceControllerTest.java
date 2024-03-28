package com.provoly.exec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.exec.*;
import com.provoly.test.AuthService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
public class JobInstanceControllerTest {
    private static final String EXPECTED_IMAGE_NAME = "imageName";
    @Inject
    JobInstanceService jobInstanceService;

    @Inject
    JobModelService jobModelService;

    @Inject
    JobInstanceController jobInstanceController;

    @Inject
    AuthService authService;

    @Inject
    JobExecutionService jobExecutionService;

    @BeforeEach
    public void authenticate() {
        authService.authenticate();
    }

    @AfterEach
    public void cleanAllJobInstances() {
        for (JobExecutionDetailsDto thisJobExecution : jobExecutionService.getAll()) {
            jobExecutionService.delete(thisJobExecution.getId());
        }
        for (JobInstanceDetailsDto thisJob : jobInstanceService.getAllInstances()) {
            jobInstanceService.deleteById(thisJob.getId());
        }
        for (JobModelDto thisJob : jobModelService.getAll()) {
            jobModelService.deleteJobModelById(thisJob.getId());
        }
    }

    private JobInstanceDto createOneJobInstances() {
        //creating job model, needed for instances
        String EXPECTED_FILENAME = "transfo.json";
        Set<ParameterDto> parameters = Set.of(new ParameterFileDto(EXPECTED_IMAGE_NAME, EXPECTED_FILENAME));
        var jobModelDto = new JobModelDto(UUID.randomUUID(), EXPECTED_IMAGE_NAME, parameters);
        jobModelService.save(jobModelDto);

        JobInstanceDto jobInstanceDto = new JobInstanceBuilder().build(jobModelDto.getId());

        jobInstanceService.save(jobInstanceDto);
        return jobInstanceDto;
    }

    @Test
    public void should_getAllExistingJobInstances() {

        var firstJobInstance = createOneJobInstances();
        var secondJobInstance = createOneJobInstances();
        var collection = jobInstanceController.getAll();

        assertThat(collection).hasSize(2);
        assertThat(collection)
                .extracting(JobInstanceDetailsDto::getId)
                .containsExactlyInAnyOrder(firstJobInstance.getId(), secondJobInstance.getId());
    }

    @Test
    public void should_returnEmptyCollectionWhenNoJobInstance() {
        assertThat(jobInstanceController.getAll()).isEmpty();
    }

    @Test
    public void should_deleteJobInstanceByIdWhenJobInstanceExist() {
        JobInstanceDto myJobInstance = createOneJobInstances();
        jobInstanceController.deleteById(myJobInstance.getId());

        assertThatThrownBy(() -> jobInstanceController.get(myJobInstance.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JobInstance : %s inexistant.".formatted(myJobInstance.getId()));
    }

    @Test
    public void should_notDeleteJobInstancesWhenDoNotExist() {

        UUID randomUUID = UUID.randomUUID();
        assertThatThrownBy(() -> jobInstanceController.deleteById(randomUUID))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("JobInstance : %s inexistant.".formatted(randomUUID));

    }

    @Test
    public void should_activateJobInstanceWhenDeactivated() {
        JobInstanceDto myJobInstance = createOneJobInstances();
        jobInstanceService.deactivate(myJobInstance.getId());

        jobInstanceController.activate(myJobInstance.getId());
        JobInstanceDetailsDto jobInstance = jobInstanceService.getInstance(myJobInstance.getId());

        assertThat(jobInstance.isActive()).isTrue();
    }

    @Test
    public void should_notActivateJobInstanceWhenActivated() {
        JobInstanceDto myJobInstance = createOneJobInstances();

        assertThatThrownBy(() -> jobInstanceController.activate(myJobInstance.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Job instance is already active");
    }

    @Test
    public void should_deactivateJobInstanceWhenActivated() {
        JobInstanceDto jobInstanceDto = createOneJobInstances();

        jobInstanceController.deactivate(jobInstanceDto.getId());

        JobInstanceDetailsDto jobInstanceDetailsDto = jobInstanceService.getInstance(jobInstanceDto.getId());
        assertThat(jobInstanceDetailsDto.isActive()).isFalse();
    }

    @Test
    public void should_notDeactivateJobInstanceWhenAlreadyDeactivated() {
        JobInstanceDto myJobInstance = createOneJobInstances();
        jobInstanceService.deactivate(myJobInstance.getId());

        assertThatThrownBy(() -> jobInstanceController.deactivate(myJobInstance.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Job instance is already inactive");
    }

    @Test
    public void should_returnLatestJobExecutionOfAJobInstance() {
        JobInstanceDto myJobInstance = createOneJobInstances();
        UUID jobInstanceId = myJobInstance.getId();

        jobInstanceController.start(jobInstanceId);
        var secondJobExecution = jobInstanceController.start(jobInstanceId);

        var lastJobExecution = jobInstanceController.getLastJobExecution(jobInstanceId);

        assertThat(lastJobExecution.getId()).isEqualTo(secondJobExecution.getId());
    }

    @Test
    public void should_notReturnAnyJobExecutionOfAJobInstance() {
        JobInstanceDto myJobInstance = createOneJobInstances();

        assertThatThrownBy(() -> jobInstanceController.getLastJobExecution(myJobInstance.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No JobExecution found for JobInstance with id %s".formatted(myJobInstance.getId()));
    }

    @Test
    public void should_startAJobInstance() {
        JobInstanceDto myJobInstance = createOneJobInstances();

        var jobExecutionDetailsDto = jobInstanceController.start(myJobInstance.getId());

        assertThat(jobExecutionDetailsDto.getExecutionDate()).isNotNull();
        assertThat(jobExecutionDetailsDto.getStatus()).isEqualTo(ExecutionStatus.STARTED);
    }

    @Test
    public void should_notStartAJobInstanceWithIncorrectId() {

        UUID incorrectJobInstanceId = UUID.randomUUID();

        assertThatThrownBy(() -> jobInstanceController.start(incorrectJobInstanceId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("JobInstance : %s inexistant.".formatted(incorrectJobInstanceId));
    }

}
