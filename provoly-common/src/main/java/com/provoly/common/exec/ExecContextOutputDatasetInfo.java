package com.provoly.common.exec;

import java.util.UUID;

import com.provoly.common.dataset.DatasetDetailsDto;

public record ExecContextOutputDatasetInfo(String topicName, UUID datasetId, DatasetDetailsDto datasetDetailDto) {
    @Override
    public String toString() {
        return "ExecContextOutputDatasetInfo{" +
                "topicName=" + topicName +
                ", datasetId='" + datasetId.toString() + '\'' +
                ", datasetDto=" + datasetDetailDto.toString() +
                '}';
    }

}
