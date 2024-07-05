package com.provoly.virt.imports;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.Role;
import com.provoly.virt.DataVirtProperties;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/imports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportsController {

    private ImportService importService;
    private Logger log;
    private DataVirtProperties dataVirtProperties;

    ImportsController(ImportService importService, Logger log, DataVirtProperties dataVirtProperties) {
        this.importService = importService;
        this.log = log;
        this.dataVirtProperties = dataVirtProperties;
    }

    @POST
    @Path("/dataset/id/{datasetId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public DatasetVersionDetailsDto importNewDataset(
            UUID datasetId,
            @RestForm FileUpload file, @RestForm boolean normalizeGeo, @RestForm int chunkSize,
            @RestForm @PartType(MediaType.APPLICATION_JSON) DatasetVersionInformationDto datasetVersionInformation) {
        if (file == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is mandatory");
        }
        if (datasetVersionInformation == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "datasetVersionInformation is mandatory");
        }

        return importService.runImportFromFile(datasetId,
                new ImportFileParameters(file, normalizeGeo, checkOrGetDefaultChunkSize(chunkSize)),
                datasetVersionInformation);
    }

    private int checkOrGetDefaultChunkSize(int chunkSize) {
        if (chunkSize == 0) {
            return dataVirtProperties.importChunkSize();
        }
        if (chunkSize < 0 || chunkSize > dataVirtProperties.maxImportChunkSize()) {
            log.info("Chunk size can't be negative or exceed %s.".formatted(dataVirtProperties.maxImportChunkSize()));
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Chunk size can't be negative or exceed %s.".formatted(dataVirtProperties.maxImportChunkSize()));
        }
        return chunkSize;
    }
}
