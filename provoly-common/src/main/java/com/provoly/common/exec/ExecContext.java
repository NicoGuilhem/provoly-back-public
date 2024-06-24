package com.provoly.common.exec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

/**
 * Use for every runtime information about a jobExecution
 */
public class ExecContext {

    private final Map<UUID, String> inTopicNameByDataSourceId = new HashMap<>();
    private final Map<UUID, ExecContextOutputDatasetInfo> outTopicNameByDatasetId = new HashMap<>();

    public void addInTopic(UUID dataSourceId, String topicName) {
        var previous = inTopicNameByDataSourceId.put(dataSourceId, topicName);
        if (previous != null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "A previous in topic with name " + previous + " already exists for dataSource " + dataSourceId);
        }
    }

    public String getInTopicName(UUID dataSourceId) {
        return inTopicNameByDataSourceId.get(dataSourceId);
    }

    public void addOutTopic(String topicName, UUID datasetId, DatasetDetailsDto datasetDetailDto) {
        var outputDatasetInfo = new ExecContextOutputDatasetInfo(topicName, datasetId, datasetDetailDto);
        var previous = outTopicNameByDatasetId.put(datasetDetailDto.getId(), outputDatasetInfo);
        if (previous != null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "A previous out topic with name " + previous + " already exists for dataSource "
                            + datasetDetailDto.getId());
        }
    }

    public ExecContextOutputDatasetInfo getOutputDatasetInfo(UUID datasetId) {
        return outTopicNameByDatasetId.get(datasetId);
    }

    public Map<UUID, String> getInTopicNameByDataSourceId() {
        return Collections.unmodifiableMap(inTopicNameByDataSourceId);
    }

    public Map<UUID, ExecContextOutputDatasetInfo> getOutTopicNameByDatasetId() {
        return Collections.unmodifiableMap(outTopicNameByDatasetId);
    }

    @Override
    public String toString() {
        return "ExecContext{" +
                "inTopicNameByDataSourceId=" + inTopicNameByDataSourceId +
                ", outTopicNameByDatasetId=" + outTopicNameByDatasetId +
                '}';
    }

}
