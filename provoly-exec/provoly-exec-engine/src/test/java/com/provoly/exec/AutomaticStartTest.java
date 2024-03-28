package com.provoly.exec;

import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASOURCE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.exec.JobInstanceBuilder;
import com.provoly.common.exec.JobModelDto;
import com.provoly.common.exec.ProvidingMethod;
import com.provoly.common.ref.RefChangeEvent;
import com.provoly.common.ref.RefChangeEventDatasetVersionActivated;
import com.provoly.test.AuthService;

import io.fabric8.kubernetes.api.model.Container;
import io.quarkus.test.junit.QuarkusTest;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AutomaticStartTest extends AbstractStartTest {

    @Inject
    AuthService authService;

    @AfterEach
    public void cleanAllTopics() {
        companion.topics().delete(companion.topics().list());
    }

    @Test
    public void automaticStartJobWhenDatasetArrive() {
        authService.authenticate();
        // Manually start a job
        var jobModelDto = new JobModelDto(UUID.randomUUID(), EXPECTED_IMAGE_NAME);
        jobModelController.save(jobModelDto);

        var jobInstanceDto = new JobInstanceBuilder()
                .withDataSource(ProvidingMethod.KAFKA_TOPIC, BIKE_STATION_DATASOURCE_ID)
                .build(jobModelDto.getId());
        jobInstanceController.save(jobInstanceDto);

        var datasetId = UUID.randomUUID();
        var eventDatasetAvailable = new RefChangeEventDatasetVersionActivated(BIKE_STATION_DATASOURCE_ID, datasetId);
        companion.produce(RefChangeEvent.class)
                .fromRecords(new ProducerRecord<>(RefChangeEvent.TOPIC_NAME, eventDatasetAvailable)).awaitCompletion();

        // TODO : wait for a job with a correct job instance annotation
        await().untilAsserted(() -> assertThat(kube.batch().v1().jobs().list().getItems())
                .flatMap(job -> job.getSpec().getTemplate().getSpec().getContainers())
                .map(Container::getImage)
                .contains(EXPECTED_IMAGE_NAME));
    }

}
