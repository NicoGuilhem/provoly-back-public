package com.provoly.transfo.engine;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.EntityIdService;
import com.provoly.clients.DataSourceService;
import com.provoly.clients.ExecService;
import com.provoly.clients.ModelService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.*;
import com.provoly.common.transfo.*;
import com.provoly.transfo.Transfo;
import com.provoly.transfo.TransfoMapper;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class TransfoService extends EntityIdService {

    @Inject
    Logger log;

    @Inject
    @RestClient
    DataSourceService dataSourceService;

    @Inject
    @RestClient
    ModelService modelService;

    @Inject
    TransfoMapper transfoMapper;

    @Inject
    @RestClient
    ExecService execService;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "provoly.transfo.job-model-id")
    UUID jobModelId;

    @Transactional
    public TransfoDetailsDto get(UUID id) {
        log.infof("getting transfo with id %s", id);
        return transfoMapper.toDetailsDto(getById(id, Transfo.class));
    }

    public Collection<TransfoDetailsDto> getAll() {
        log.info("listing all transfo");
        return transfoMapper.toDetailsDto(getAll(Transfo.class));
    }

    public void delete(UUID id) {
        log.infof("deleting transfo with id %s", id);
        removeEntity(id, Transfo.class);
    }

    @Transactional
    public TransfoStatus saveAndValid(TransfoDto transfoDto) {
        log.info("Validating transfo");

        Optional<Transfo> optionalTransfo = findById(transfoDto.getId(), Transfo.class);

        optionalTransfo.ifPresent(this::canUpdate);

        TransfoValidator graph = new TransfoValidator(transfoDto);
        TransfoStatus status = graph.validate(dataSourceService, modelService);

        var transfoToSave = transfoMapper.toEntity(transfoDto);

        optionalTransfo.ifPresentOrElse(
                transfo -> updateTransfo(transfoToSave),
                () -> createTransfo(transfoToSave));

        return status;
    }

    public boolean isTransfoValid(TransfoDto transfoDto) {
        var transfoValidator = new TransfoValidator(transfoDto);
        var transfoStatus = transfoValidator.validate(dataSourceService, modelService);
        return transfoStatus.getErrors().isEmpty() && !transfoDto.getNodes().isEmpty() && !transfoDto.getLinks().isEmpty();
    }

    @Transactional
    public void activate(UUID id) {
        Transfo transfo = getById(id, Transfo.class);
        TransfoDto transfoDto = transfoMapper.toDto(transfo);
        JobInstanceDto jobInstanceDto;

        if (isTransfoValid(transfoDto)) {
            transfo.setActive(true);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Can't activate invalid transformation %s.".formatted(id));
        }
        if (transfo.getJobInstanceId() == null) {
            jobInstanceDto = new JobInstanceDto(
                    UUID.randomUUID(),
                    jobModelId,
                    getDataSourceProvidingDtos(transfoDto),
                    getDatasetOutcomeDtos(transfoDto),
                    getParameterValueDto(transfoDto));
            log.infof("generate a job instance for transfo %s", id);

            execService.createInstance(jobInstanceDto);
            log.infof("Call exec engine to save new job instance with id %s", jobInstanceDto.getId());
            transfo.setJobInstanceId(jobInstanceDto.getId());
        } else {
            execService.activate(transfo.getJobInstanceId());
        }
    }

    @Transactional
    public void deactivate(UUID id) {
        Transfo transfo = getById(id, Transfo.class);

        transfo.setActive(false);
        if (transfo.getJobInstanceId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "You have to activate Transfo first.");
        }
        log.infof("Call exec engine to deactivate new job instance with id %s", transfo.getJobInstanceId());
        execService.deactivate(transfo.getJobInstanceId());
    }

    public TransfoStatus getStatus(UUID transfoId) {
        log.infof("Requesting transfo status :%s", transfoId);
        TransfoDto transfoDto = get(transfoId);
        var graph = new TransfoValidator(transfoDto);
        return graph.validate(dataSourceService, modelService);
    }

    private void canUpdate(Transfo transfo) {
        if (transfo.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Active transformation can't be modified.");
        }
    }

    private void createTransfo(Transfo transfo) {
        log.infof("Creating transfo with id %s", transfo.getId());
        persist(transfo);
    }

    private void updateTransfo(Transfo transfo) {
        log.infof("Transfo with id %s already exists, updating it", transfo.getId());
        // Merge is needed because nodes and links are also in th persistence context.
        em.merge(transfo);
    }

    private Set<DataSourceProvidingDto> getDataSourceProvidingDtos(TransfoDto transfoDto) {
        Set<DataSourceProvidingDto> inDatasources = new HashSet<>();
        getSourceIds(transfoDto, InputDatasource.class.getName())
                .forEach(inputDs -> {
                    var ds = (InputDatasource) inputDs.getSpec();
                    inDatasources.add(new DataSourceProvidingDto(ProvidingMethod.KAFKA_TOPIC, ds.getDatasetId()));
                });

        return inDatasources;
    }

    private Set<DatasetOutcomeDto> getDatasetOutcomeDtos(TransfoDto transfoDto) {
        Set<DatasetOutcomeDto> datasetOutcomes = new HashSet<>();
        getSourceIds(transfoDto, OutputDataset.class.getName())
                .forEach(outputDs -> {
                    var ds = (OutputDataset) outputDs.getSpec();
                    datasetOutcomes.add(new DatasetOutcomeDto(OutcomeMethod.KAFKA_TOPIC, ds.getDataset()));
                });
        return datasetOutcomes;
    }

    private Set<ParameterValueDto> getParameterValueDto(TransfoDto transfoDto) {
        try {
            return Set.of(new ParameterValueDto("transformation_file", mapper.writeValueAsString(transfoDto)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<NodeDto> getSourceIds(TransfoDto transfoDto, String data) {
        return transfoDto.getNodes()
                .stream()
                .filter(nodeDto -> nodeDto.getType().equals(data))
                .toList();
    }
}
