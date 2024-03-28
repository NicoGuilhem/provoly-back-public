package com.provoly.virt.event;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.imports.ImportsMessage;
import com.provoly.common.virt.*;
import com.provoly.common.virt.VirtChangeEvent;
import com.provoly.common.virt.VirtChangeEventImportMessageCreated;
import com.provoly.common.virt.VirtChangeEventUpdateDatasetVersionState;
import com.provoly.common.virt.VirtChangeEventUpdateDatasetVersionStateAndMessageCreated;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class VirtEventEmitter {
    private Emitter<VirtChangeEvent> emitter;

    public VirtEventEmitter(@Channel(VirtChangeEvent.TOPIC_NAME) Emitter<VirtChangeEvent> emitter) {
        this.emitter = emitter;
    }

    public void sendImportMessage(ImportsMessage importsMessage) {
        emitter.send(new VirtChangeEventImportMessageCreated(importsMessage));
    }

    public void sendDatasetVersion(DatasetVersionDto datasetVersionDto) {
        emitter.send(new VirtChangeEventUpdateDatasetVersionState(datasetVersionDto));
    }

    public void sendDatasetVersionAndImportMessage(DatasetVersionDto datasetVersionDto, ImportsMessage importsMessage) {
        emitter.send(new VirtChangeEventUpdateDatasetVersionStateAndMessageCreated(datasetVersionDto, importsMessage));
    }

    public void sendDatasetVersionDelete(VirtChangeEvent.Type type, DatasetVersionDto datasetVersionDto) {
        if (type == VirtChangeEvent.Type.DELETE_DATASET_VERSION_ERROR) {
            emitter.send(new VirtChangeEventDeleteDatasetVersionError(datasetVersionDto.getId()));
        } else if (type == VirtChangeEvent.Type.DELETED_DATASET_VERSION) {
            emitter.send(new VirtChangeEventDatasetVersionDeleted(datasetVersionDto.getId()));
        }
    }
}
