package com.provoly.transfo.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.provoly.common.search.Operator;
import com.provoly.common.transfo.Filter;

import io.quarkus.test.junit.QuarkusTest;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FilterTest extends AbstractTransformerTest {

    /**
     * Test a simple condition on an attribute
     */
    @Test
    void filter_removeFullStation() {

        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 0);

        var transfo = buildTransfo(filter);
        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults()).hasSize(2);
    }

    @Test
    void filter_removeBigStation() {

        var filter = new Filter("totalSpace", Operator.LOWER_THAN, 20);

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("0", "11");
    }

    @Test
    void filter_equals_operator() {

        var filter = new Filter("totalSpace", Operator.EQUALS, 12);

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("11");
    }

    @Test
    void filter_notEquals_operator() {

        var filter = new Filter("totalSpace", Operator.NOT_EQUALS, 12);

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("0", "12");
    }

    @Test
    void filter_startWith_operator() {

        var filter = new Filter("totalSpace", Operator.START_WITH, 1);

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("11");
    }

    @Test
    void filter_endWith_operator() {

        var filter = new Filter("totalSpace", Operator.END_WITH, "3");

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("12");
    }

    @Test
    void filter_contains_operator() {

        var filter = new Filter("totalSpace", Operator.CONTAINS, "2");

        var transfo = buildTransfo(filter);

        runTransfo(transfo);

        // Expect runner processes items
        assertThat(getResults())
                .extracting(ConsumerRecord::value)
                .extracting(this::getFreeSpaceValue)
                .containsExactlyInAnyOrder("12", "11");
    }
}
