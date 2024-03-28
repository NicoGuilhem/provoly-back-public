package com.provoly.transfo.engine;

import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASOURCE_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.clients.ExecService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.exec.ExecutionStatus;
import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.exec.JobInstanceDetailsDto;
import com.provoly.common.search.Operator;
import com.provoly.common.transfo.*;
import com.provoly.transfo.Transfo;
import com.provoly.transfo.TransfoMapper;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class TransfoControllerTest {
    private static final String JOB_TRANSFO_URL = "/transfos";
    private static final UUID transfoId = UUID.randomUUID();

    @Inject
    TransfoController controller;
    @Inject
    TransfoService service;
    @Inject
    TransfoMapper transfoMapper;

    @InjectMock
    @RestClient
    ExecService execService;

    public TransfoDto createTransfo() {
        var filter = new Filter("toto", Operator.LOWER_THAN, 2);
        var nodeDto = new NodeDto(UUID.randomUUID(), filter);
        return TransfoDto.withLinkGeneration(transfoId, List.of(nodeDto), "Titre");
    }

    public TransfoDto createTransfoErrors() {
        return new TransfoDto(transfoId, Set.of(), Set.of(), "Titre");
    }

    public TransfoDto createTransfoBike() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        return TransfoDto.withLinkGeneration(transfoId, List.of(nodeInput, nodeFilter), "Titre");
    }

    @AfterEach
    public void clean() {
        if (service.checkEntityExists(transfoId, Transfo.class)) {
            service.removeEntity(transfoId, Transfo.class);
        }
    }

    @Test
    public void addTransfoWithFilter() {
        service.saveAndValid(createTransfo());

        var transfoResponse = controller.get(transfoId);

        BDDAssertions.then(transfoResponse.getLinks()).isEmpty();
        BDDAssertions.then(transfoResponse.getNodes()).hasSize(1);

        var node = transfoResponse.getNodes().stream().toList().get(0);

        BDDAssertions.then(node.getType()).isEqualTo(Filter.class.getName());
        BDDAssertions.then(node.getSpec()).isInstanceOf(Filter.class);

        var spec = (Filter) node.getSpec();
        BDDAssertions.then(spec.getAttributeName()).isEqualTo("toto");
    }

    @Test
    public void deleteExistingTransfo() {
        // Given
        service.saveAndValid(createTransfoBike());

        // When
        controller.delete(transfoId);

        // Then
        BDDAssertions.then(controller.list()).isEmpty();
    }

    @Test
    public void updateTransfo() {
        // Given
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(transfoId, List.of(nodeInput, nodeFilter), "Titre");

        controller.saveAndValid(transfo);
        var creationDate = controller.get(transfoId).getCreationDate();

        // When
        transfo = TransfoDto.withLinkGeneration(transfoId, List.of(nodeInput, nodeFilter), "Titre2");
        controller.saveAndValid(transfo);

        // Then
        var transfoResponse = controller.get(transfoId);
        BDDAssertions.then(transfoResponse.getLinks()).hasSize(1);
        BDDAssertions.then(transfoResponse.getTitle()).isEqualTo("Titre2");
        BDDAssertions.then(transfoResponse.getCreationDate()).isEqualTo(creationDate);
    }

    @Test
    public void should_addJobInstanceIdInTransfoWhenActivateTransfo() {
        // Given
        var transfo = createTransfoBike();
        service.saveAndValid(transfo);

        // When
        controller.activate(transfoId);
        var transfoDtoUpdated = controller.get(transfoId);

        // Then
        BDDAssertions.then(transfoDtoUpdated.getJobInstanceId()).isNotNull();
    }

    @Test
    public void should_throwErrorWhenDeleteMissingTransfo_return404() {
        assertThatThrownBy(() -> controller.delete(transfoId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("Transfo : %s inexistant.".formatted(transfoId));
    }

    @Test
    public void should_generateJobInstanceOnFirstActivate_returnID() {
        // Given
        TransfoDto transfoDto = createTransfoBike();
        var transfo = transfoMapper.toEntity(transfoDto);
        controller.saveAndValid(transfoDto);
        BDDAssertions.then(controller.get(transfo.getId()).getJobInstanceId()).isNull();

        // When
        controller.activate(transfoDto.getId());
        TransfoDetailsDto result = controller.get(transfoDto.getId());

        // Then
        BDDAssertions.then(result.getJobInstanceId()).isNotNull();
    }

    @Test
    public void should_ThrowErrorWhenDeactivateANeverActivatedTransformation_return400() {
        TransfoDto transfoDto = createTransfoBike();
        service.saveAndValid(transfoDto);

        assertThatThrownBy(() -> controller.deactivate(transfoId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You have to activate Transfo first.");
    }

    @Test
    public void should_setActiveParamsTrueWhenActivateTransfo() {
        // Given
        TransfoDto transfoDto = createTransfoBike();
        var transfo = transfoMapper.toEntity(transfoDto);
        service.saveAndValid(transfoDto);

        // When
        controller.activate(transfoDto.getId());
        TransfoDetailsDto result = controller.get(transfo.getId());

        // Then
        BDDAssertions.then(result.isActive()).isTrue();
    }

    @Test
    public void should_setActiveParamsFalseWhenDeactivateTransfo() {

        // Given
        TransfoDto transfoDto = createTransfoBike();
        var transfo = transfoMapper.toEntity(transfoDto);
        service.saveAndValid(transfoDto);

        // When
        controller.activate(transfoDto.getId());
        controller.deactivate(transfoDto.getId());

        // Then
        TransfoDetailsDto result = controller.get(transfo.getId());
        BDDAssertions.then(result.isActive()).isFalse();
    }

    @Test
    public void should_throwError_whenActivateTransfoWithoutNodesAndLinks() {
        TransfoDto transfoDto = createTransfoErrors();
        controller.saveAndValid(transfoDto);

        assertThatThrownBy(() -> controller.activate(transfoDto.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Can't activate invalid transformation %s.".formatted(transfoDto.getId()));
    }

    @Test
    public void should_getLastJobExecutionWhenExists_returnDto() {
        // Given
        UUID jobExecutionId = UUID.randomUUID();
        Instant jobExecutionDate = Instant.now();
        TransfoDto transfoDto = createTransfoBike();
        service.saveAndValid(transfoDto);
        service.activate(transfoId);
        var jobInstanceId = service.get(transfoId).getJobInstanceId();

        // When
        Mockito.when(execService.getLastJobExecution(jobInstanceId))
                .thenReturn(new JobExecutionDetailsDto(jobExecutionId,
                        new JobInstanceDetailsDto(jobInstanceId, null, null, null, null, true), ExecutionStatus.STARTED,
                        jobExecutionDate));
        TransfoDetailsDto result = controller.get(transfoDto.getId());

        // Then
        BDDAssertions.then(result.getLastJobExecution()).isNotNull();
        BDDAssertions.then(result.getLastJobExecution().getId()).isEqualTo(jobExecutionId);
        BDDAssertions.then(result.getLastJobExecution().getExecutionDate()).isEqualTo(jobExecutionDate);
        BDDAssertions.then(result.getLastJobExecution().getStatus()).isEqualTo(ExecutionStatus.STARTED);
    }

    @Test
    public void should_returnNullWhenInstanceNotActive() {
        // Given
        TransfoDto transfoDto = createTransfoBike();
        controller.saveAndValid(transfoDto);

        // When
        TransfoDetailsDto result = controller.get(transfoDto.getId());

        // Then
        BDDAssertions.then(result.getLastJobExecution()).isNull();
    }
}