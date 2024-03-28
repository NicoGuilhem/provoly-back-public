package com.provoly.common.exec;

import java.util.UUID;

import com.provoly.common.dataset.DatasetDto;

public record ExecContextOutputDatasetInfo(String topicName, UUID datasetId, DatasetDto datasetDto) {
    @Override
    public String toString() {
        return "ExecContextOutputDatasetInfo{" +
                "topicName=" + topicName +
                ", datasetId='" + datasetId.toString() + '\'' +
                ", datasetDto=" + datasetDto.toString() +
                '}';
    }

}
