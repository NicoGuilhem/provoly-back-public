package com.provoly.virt.storage;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.model.OClassDetailsDto;

public interface StorageModelService {
    void createOClass(OClassDetailsDto oClass);

    void updateOClass(OClassDetailsDto oClass);

    void deleteOClass(OClassDetailsDto oClass);

    void deleteDatasetVersion(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto);
}
