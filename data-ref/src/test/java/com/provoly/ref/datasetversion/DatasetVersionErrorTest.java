package com.provoly.ref.datasetversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.imports.*;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetMapper;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DatasetVersionErrorTest {

    @Inject
    EntityIdService entityIdService;
    @Inject
    DatasetVersionService datasetVersionService;
    @Inject
    DatasetVersionMessageService datasetVersionMessageService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    ModelService modelService;
    @Inject
    DatasetMapper datasetMapper;
    @Inject
    DatasetVersionMapper datasetVersionMapper;
    @Inject
    DatasetRepository datasetRepository;
    @Inject
    TestService testService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    MetadataService metadataService;
    @Inject
    UserService userService;

    private DatasetDto datasetDto;
    private DatasetVersionDto datasetVersionDto;
    private OClass oClass;

    void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        ProvolyUser provolyUser = userService.getCurrentUser();
        OClassWriteDto oclassWriteDto = new OClassWriteDto(UUID.randomUUID(), "datasetVersionTest", new ArrayList<>(),
                Storage.ELASTIC);
        oClass = modelMapper.toModel(oclassWriteDto);
        if (!modelService.exists(oClass)) {
            modelService.saveEntity(oClass);
        }

        // add dataset
        datasetDto = new DatasetDto(
                UUID.randomUUID(),
                "name",
                oclassWriteDto.getId(),
                DatasetType.CLOSED);
        datasetVersionDto = new DatasetVersionDto(UUID.randomUUID(),
                datasetDto.getId(), oClass.getId(), DatasetState.ACTIVE, "author", Instant.now());

        Dataset dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetRepository.save(dataset);
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersionDto));
        datasetVersionService.activateDatasetVersion(datasetVersionDto.getId());
    }

    @AfterEach
    void delete() {
        testService.clean();
    }

    @Test
    public void test_save_DatasetVersionError_success() {
        //  GIVEN
        init();
        var extractedError = new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.FORMAT,
                new FileImportDto.ParamsTypeError("name"));
        var extractedMessage = new ExtractedMessage(MessageLevel.WARNING, ExtractMessageCode.FORMAT,
                new FileImportDto.ParamsTypeError("name"));
        var importError = new ImportsMessage(datasetVersionDto.getId(), "1", List.of(extractedError, extractedMessage));

        // WHEN
        datasetVersionMessageService.save(importError);

        // THEN
        var datasetVersionErrorList = entityIdService.getAll(DatasetVersionMessage.class);
        assertThat(datasetVersionErrorList).hasSameSizeAs(List.of(extractedError, extractedMessage));

        var datasetVersionError = datasetVersionErrorList.get(0);
        assertThat(datasetVersionError.getExtractMessageCode()).isEqualTo(extractedError.code());
        assertThat(datasetVersionError.getName()).isEqualTo(extractedError.params().name());
        assertThat(datasetVersionError.getType()).isEqualTo(extractedError.params().type());
    }
}