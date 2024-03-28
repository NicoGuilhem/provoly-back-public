package com.provoly.virt.test;

import static com.provoly.common.Storage.KUZZLE_ASSET;
import static com.provoly.common.Storage.KUZZLE_MEASURE;
import static org.awaitility.Awaitility.await;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.item.ItemDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.KuzzleDeviceManagerClient;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

@ApplicationScoped
public class KuzzleTestService {

    private DataVirtProperties dataVirtProperties;
    public static final Boolean KUZZLE_ENABLED = Boolean.parseBoolean(System.getProperty("service.kuzzle.enabled", "false"));
    public static final String ASSET_MODEL = "AssetTest";
    public static final String MEASURE_MODEL = "measureTest";
    public static final List<Map<String, String>> MEASURE_CONFIGURATION = List
            .of(Map.of("name", MEASURE_MODEL, "type", MEASURE_MODEL));
    public static final String DEVICE_MODEL = "DeviceTest";
    private KuzzleDeviceManagerClient kuzzleDeviceManagerClient;
    private String engine;
    private Logger log;

    public KuzzleTestService(DataVirtProperties dataVirtProperties, Logger log)
            throws URISyntaxException, MalformedURLException {
        this.log = log;
        this.dataVirtProperties = dataVirtProperties;
        this.kuzzleDeviceManagerClient = RestClientBuilder.newBuilder()
                .baseUrl(new URI(this.dataVirtProperties.kuzzle().kuzzleUrl().orElse("http://localhost:7512")).toURL())
                .build(KuzzleDeviceManagerClient.class);
        this.engine = dataVirtProperties.kuzzle().tenant().orElse("test");

    }

    public void createEngine() {
        kuzzleDeviceManagerClient.createEngine(engine);
        await().until(() -> kuzzleDeviceManagerClient.getEngine(engine).getStatus() == 200);
        log.infof("Engine %s created", engine);
    }

    public void clearKuzzle(String... deviceIds) {
        if (KUZZLE_ENABLED) {
            kuzzleDeviceManagerClient.deleteEngine(engine);
            log.infof("Engine %s deleted", engine);
            kuzzleDeviceManagerClient.deleteIndex(engine);
            log.infof("Index %s deleted", engine);

            for (var deviceId : List.of(deviceIds)) {
                kuzzleDeviceManagerClient.deleteDevice("%s-%s".formatted(DEVICE_MODEL, deviceId));
            }
            log.infof("Devices deleted");
        }
    }

    public void initKuzzleModels(Map<String, Object> model) {
        createEngine();
        createMeasureModel(model);
        createAssetModel(model);
        createDeviceModel();
    }

    public void createMeasureModel(Map<String, Object> attributes) {
        kuzzleDeviceManagerClient.createMeasureModel(Map.of(
                "engineGroup", "commons",
                "type", MEASURE_MODEL,
                "valuesMappings", attributes));
        log.infof("Measure Model %s created", MEASURE_MODEL);
    }

    public void createAssetModel(Map<String, Object> attributes) {
        kuzzleDeviceManagerClient.createAssetModel(Map.of(
                "engineGroup", "commons",
                "model", ASSET_MODEL,
                "metadataMappings", attributes,
                "measures", MEASURE_CONFIGURATION));
        log.infof("Asset model %s created", ASSET_MODEL);
    }

    public void createDeviceModel() {
        kuzzleDeviceManagerClient.createDeviceModel(Map.of(
                "engineGroup", "commons",
                "model", DEVICE_MODEL,
                "measures", MEASURE_CONFIGURATION));
        log.infof("Device model %s created", DEVICE_MODEL);

    }

    public void insertAssetItem(String assetId, Map<String, Object> attributes) {
        kuzzleDeviceManagerClient.createAsset(engine, Map.of(
                "_id", assetId,
                "model", ASSET_MODEL,
                "reference", assetId,
                "metadata", attributes));
        await().until(
                () -> kuzzleDeviceManagerClient.getAsset(engine, "%s-%s".formatted(ASSET_MODEL, assetId)).getStatus() == 200);
        log.infof("Asset doc %s inserted", assetId);
    }

    public void insertDeviceItem(String deviceId) {
        kuzzleDeviceManagerClient.createDevice(engine, Map.of(
                "_id", deviceId,
                "model", DEVICE_MODEL,
                "reference", deviceId));
        await().until(() -> kuzzleDeviceManagerClient.getDevice("%s-%s".formatted(DEVICE_MODEL, deviceId)).getStatus() == 200);
        log.infof("Device doc %s inserted", deviceId);
    }

    public void insertMeasureItem(String deviceId, Map<String, Object> attributes) {
        kuzzleDeviceManagerClient.receiveMeasure(engine, "%s-%s".formatted(DEVICE_MODEL, deviceId), Map.of(
                "measures", List.of(Map.of(
                        "measureName", MEASURE_MODEL,
                        "type", MEASURE_MODEL,
                        "values", attributes))));
        await().until(() -> kuzzleDeviceManagerClient.getMeasuresForDevice(engine, "%s-%s".formatted(DEVICE_MODEL, deviceId))
                .getStatus() == 200);
        log.infof("Measure received for device %s", deviceId);
    }

    public ItemDto insertKuzzleItem(String deviceId, String assetId, Map<String, Object> attribute, Storage storage,
            DatasetVersionDto dsVersion) {
        try {
            kuzzleDeviceManagerClient.getAsset(engine, "%s-%s".formatted(ASSET_MODEL, assetId));
        } catch (ClientWebApplicationException e) {
            log.infof("Asset %s not exists, insert it", assetId);
            insertAssetItem(assetId, attribute);
        }

        if (storage == KUZZLE_ASSET) {
            return new ItemDto(dsVersion.getoClass(), dsVersion.getId() + "@" + assetId);
        }
        if (storage == KUZZLE_MEASURE) {
            insertDeviceItem(deviceId);
            kuzzleDeviceManagerClient.linkDeviceToAsset(engine,
                    "%s-%s".formatted(DEVICE_MODEL, deviceId), "%s-%s".formatted(ASSET_MODEL, assetId),
                    Map.of("measureNames", List.of(Map.of("asset", MEASURE_MODEL, "device", MEASURE_MODEL))));
            log.infof("Device doc %s link to asset doc %s", deviceId, assetId);

            insertMeasureItem(deviceId, attribute);

            /*
             * We can't use the await until because we don't know the measure id to retreive it
             */
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            /*
             * It is not possible to overload the id of a measure when it is created.
             * In addition, the creation does not return the measure created.
             * We have to retrieve the device's list of measurements to get the information.
             * We can retrieve the id of the first measurement because tests only insert one measurement per device.
             */
            log.infof("Get id for measure of device %s", deviceId);
            Map<String, Object> res = (Map<String, Object>) kuzzleDeviceManagerClient
                    .getMeasuresForDevice(engine, "%s-%s".formatted(DEVICE_MODEL, deviceId)).getEntity().get("result");
            List<Map<String, String>> measures = (List<Map<String, String>>) res.get("measures");
            return new ItemDto(dsVersion.getoClass(), dsVersion.getId() + "@" + measures.get(0).get("_id"));
        }
        return null;
    }
}
