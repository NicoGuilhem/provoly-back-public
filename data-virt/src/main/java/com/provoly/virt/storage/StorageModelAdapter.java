package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.virt.VirtChangeEvent;
import com.provoly.virt.event.VirtEventEmitter;
import com.provoly.virt.file.FileService;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageModelAdapter implements StorageModelService {

    private Logger log;
    private Instance<StorageModelService> storageModelServices;
    private FileService fileService;
    private VirtEventEmitter virtEventEmitter;

    public StorageModelAdapter(Logger log, @Any Instance<StorageModelService> storageModelServices, FileService fileService,
            VirtEventEmitter virtEventEmitter) {
        this.log = log;
        this.storageModelServices = storageModelServices;
        this.fileService = fileService;
        this.virtEventEmitter = virtEventEmitter;
    }

    @Override
    public void createOClass(OClassDetailsDto oClass) {
        log.infof("Create class %s storage", oClass);
        getService(storageModelServices, oClass.getStorage()).createOClass(oClass);
    }

    @Override
    public void updateOClass(OClassDetailsDto oClass) {
        log.infof("update class %s storage", oClass);
        getService(storageModelServices, oClass.getStorage()).updateOClass(oClass);
    }

    @Override
    public void deleteOClass(OClassDetailsDto oClass) {
        log.infof("delete class %s storage", oClass);
        getService(storageModelServices, oClass.getStorage()).deleteOClass(oClass);
    }

    @Override
    public void deleteDatasetVersion(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto) {
        log.infof("delete dataset version %s", datasetVersionDto.getId());
        try {
            if (datasetVersionDto.isWithFile()) {
                fileService.deleteRawFile(datasetVersionDto);
            }
            getService(storageModelServices, oClassDetailsDto.getStorage()).deleteDatasetVersion(datasetVersionDto,
                    oClassDetailsDto);
            log.warn("Dataset version items deleted");
            virtEventEmitter.sendDatasetVersionDelete(VirtChangeEvent.Type.DELETED_DATASET_VERSION, datasetVersionDto);
        } catch (Exception e) {
            log.errorf(e, "Error occured while deleting dataset version %s", datasetVersionDto.getId());
            virtEventEmitter.sendDatasetVersionDelete(VirtChangeEvent.Type.DELETE_DATASET_VERSION_ERROR, datasetVersionDto);
        }

    }

}
