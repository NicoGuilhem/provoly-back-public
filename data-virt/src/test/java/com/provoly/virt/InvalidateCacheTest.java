package com.provoly.virt;

import jakarta.inject.Inject;

import com.provoly.clients.ModelService;
import com.provoly.common.Storage;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.test.CacheEnabledProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestProfile(CacheEnabledProfile.class)
public class InvalidateCacheTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    @RestClient
    ModelService modelService;

    private OClassWriteDto vehicleClass;

    @AfterEach
    public void cleaning() {
        modelService.deleteClass(vehicleClass.getId());
    }

    @Test
    public void should_invalidateOClassCache_When_UpdatingOClass() {
        // given
        authService.authenticate();
        var idVehicleField = testData.createField("id_vehicle_cache", Type.KEYWORD);
        var attributeIdVehicle = testData.createAttribute("id_vehicle_cache", idVehicleField);
        vehicleClass = testData.createClass(companion, "vehicle_cache", attributeIdVehicle);
        modelService.getDetails(vehicleClass.getId());

        // when
        attributeIdVehicle.setName("id_vehicle_updated");
        testData.createClassWithId(companion, vehicleClass.getId(), "vehicle_cache", Storage.ELASTIC, null, attributeIdVehicle);
        var updatedClass = modelService.getDetails(vehicleClass.getId());

        //then
        Assertions.assertThat(updatedClass.getAttributes().get(0).getName()).isEqualTo("id_vehicle_updated");

    }
}
