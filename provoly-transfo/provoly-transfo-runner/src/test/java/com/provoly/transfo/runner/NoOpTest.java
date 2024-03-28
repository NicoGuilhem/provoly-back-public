package com.provoly.transfo.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.provoly.common.item.ItemDto;

import io.quarkus.test.junit.QuarkusTest;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NoOpTest extends AbstractTransformerTest {

    /**
     * Test simple case with simple copy without any transformation
     */
    @Test
    void emptyTransformation_do_nothing() {
        var transfo = buildTransfo();
        runTransfo(transfo);
        assertThat(getResults()).hasSize(3);
    }

    /**
     * Test case where the producer provides items more slowly than the consumer
     */
    @Test
    void whenLateArrivalOfLast() throws Exception {
        var transfo = buildTransfo();

        startTransfoRunner(transfo);
        sendStartEvent(transfo);

        log.info("Producing all records excepts last one");
        // Produce all records except last one
        List<ProducerRecord<String, ItemDto>> records = buildRecords();
        var lastRecord = records.remove(records.size() - 1);
        sendRecords(records);

        // allows time to thread to process all items and send last item
        Thread.sleep(1000);
        log.info("Producing last record");
        sendRecords(List.of(lastRecord));
        sendStopEvent(transfo);

        // Expect runner processes items
        assertThat(getResults()).hasSize(3);
    }

}
