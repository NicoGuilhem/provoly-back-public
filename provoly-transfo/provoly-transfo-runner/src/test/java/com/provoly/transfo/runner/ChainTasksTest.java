package com.provoly.transfo.runner;

import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASOURCE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.provoly.common.item.ItemDto;
import com.provoly.common.search.Operator;
import com.provoly.common.transfo.*;

import io.quarkus.test.junit.QuarkusTest;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ChainTasksTest extends AbstractTransformerTest {

    @Test
    public void chain_2filters() {

        var filter1 = new Filter("freeSpace", Operator.GREATER_THAN, 0);
        var filter2 = new Filter("totalSpace", Operator.LOWER_THAN, 20);

        var transfo = buildTransfo(filter1, filter2);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactly("11");
    }

    @Test
    public void chain_FilterAndNoOp() {
        var filter1 = new Filter("freeSpace", Operator.GREATER_THAN, 0);
        var noOp = new NoOp();

        var transfo = buildTransfo(filter1, noOp);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("12", "11");
    }

    @Test
    public void chain_twoOutputs() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);

        var filter1 = new Filter("freeSpace", Operator.GREATER_THAN, 0);
        var nodeFilter1 = new NodeDto(filter1);

        var filter2 = new Filter("freeSpace", Operator.LOWER_THAN, 1);
        var nodeFilter2 = new NodeDto(filter2);

        var output1 = new OutputDataset(UUID.randomUUID());
        var nodeOutput1 = new NodeDto(output1);
        var output2 = new OutputDataset(UUID.randomUUID());
        var nodeOutput2 = new NodeDto(output2);

        var linkInputFilter1 = new LinkDto(nodeInput.getId(), 0, nodeFilter1.getId(), 0);
        var linkFilter1Output1 = new LinkDto(nodeFilter1.getId(), 0, nodeOutput1.getId(), 0);

        var linkInputFilter2 = new LinkDto(nodeInput.getId(), 0, nodeFilter2.getId(), 0);
        var linkFilter2Output2 = new LinkDto(nodeFilter2.getId(), 0, nodeOutput2.getId(), 0);

        var transfo = new TransfoDto(transfoUuid,
                Set.of(nodeInput, nodeFilter1, nodeFilter2, nodeOutput1, nodeOutput2),
                Set.of(linkInputFilter1, linkFilter1Output1, linkInputFilter2, linkFilter2Output2),
                "Default Title");

        runTransfo(transfo);
        Map<String, List<ConsumerRecord<String, ItemDto>>> records = getResults(
                List.of(output1.getDataset(), output2.getDataset()))
                .stream()
                .collect(Collectors.groupingBy(ConsumerRecord::topic));

        // Expect runner processes items
        assertThat(records.get(getOutTopicName(output1.getDataset())))
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("12", "11");
        assertThat(records.get(getOutTopicName(output2.getDataset())))
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("0");

    }

}
