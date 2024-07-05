package com.provoly.virt.item;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetaProvisioningService;
import com.provoly.clients.MetadataRefService;
import com.provoly.clients.ModelService;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.AttributeDto;
import com.provoly.common.item.AttributeMultiValueDto;
import com.provoly.common.item.AttributeSimpleValueDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.metadata.MetaProvisioningReaderDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.model.Type;
import com.provoly.security.TokenUtils;
import com.provoly.virt.entity.*;
import com.provoly.virt.file.FileService;
import com.provoly.virt.imports.RecordConvertor;
import com.provoly.virt.imports.model.ItemRecord;
import com.provoly.virt.metadata.UserMetadataCachedService;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ItemTransformer {

    private Logger log;

    private MetadataRefService metadataService;

    private FileService fileService;

    private RecordConvertor recordConvertor;

    private ModelService modelService;

    private UserMetadataCachedService userMetadataCachedService;

    private MetaProvisioningService metaProvisioningService;

    private TokenUtils tokenUtils;

    public ItemTransformer(Logger log,
            @RestClient ModelService modelService,
            @RestClient MetadataRefService metadataService,
            FileService fileService,
            RecordConvertor recordConvertor,
            UserMetadataCachedService userMetadataCachedService,
            @RestClient MetaProvisioningService metaProvisioningService,
            TokenUtils tokenUtils) {
        this.log = log;
        this.modelService = modelService;
        this.metadataService = metadataService;
        this.fileService = fileService;
        this.recordConvertor = recordConvertor;
        this.userMetadataCachedService = userMetadataCachedService;
        this.metaProvisioningService = metaProvisioningService;
        this.tokenUtils = tokenUtils;
    }

    public List<Item> transform(DatasetDto datasetDto, List<ItemDto> chunk) {
        return chunk.stream()
                .map(itemDto -> transform(itemDto, datasetDto))
                .toList();
    }

    private Item transform(ItemDto itemDto, DatasetDto datasetDto) {
        log.tracef("ItemDto=[%s] toModel", itemDto);

        // We are not using mapper as mapping is too complicated
        var oClassDto = modelService.getDetails(itemDto.getoClass());
        ItemId itemId = new ItemId(itemDto.getId());
        Item item = new Item(itemId, oClassDto);

        Map<String, Object> itemValues = itemDto.getAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, ItemTransformer::getAttributeValue));

        ItemRecord itemRecord = new ItemRecord(itemId.getId(), itemValues);

        var parsedRecord = recordConvertor.convert(itemRecord, oClassDto);

        if (!parsedRecord.messages().isEmpty()) {
            log.errorf("There are %s errors : %s".formatted(parsedRecord.messages().size(), parsedRecord.messages()));
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "There are %s errors : %s".formatted(parsedRecord.messages().size(), parsedRecord.messages()));
        }

        for (Map.Entry<String, Object> metaEntry : itemDto.getMetadata().entrySet()) {
            var metadataDef = metadataService.getByName(metaEntry.getKey());
            item.add(new MetadataValueDto(metadataDef, metaEntry.getValue()));
        }

        for (Map.Entry<String, AttributeDto> attributeEntry : itemDto.getAttributes().entrySet()) {
            var name = attributeEntry.getKey();
            var attributeDto = attributeEntry.getValue();
            var normalizedValues = parsedRecord.record().values().get(name);

            switch (attributeDto.getType()) {
                case VALUE -> mapSimpleDtoValueToItem(item.getAttributeSimple(name), (AttributeSimpleValueDto) attributeDto,
                        normalizedValues);
                case MULTI -> mapMultiDtoValueToItem(item, name, (AttributeMultiValueDto) attributeDto); // TODO: MultiValued attribute currently instable, should consider removing or rework
                default -> throw new IllegalStateException("Unknown attribute type " + attributeDto.getType());
            }

        }
        loadRawData(item);
        LocalDateTime insertionDate = LocalDateTime.now();
        automaticMetaProvisioning(datasetDto, item, insertionDate);

        return item;
    }

    private static Object getAttributeValue(Map.Entry<String, AttributeDto> entry) {
        return switch (entry.getValue()) {
            case AttributeSimpleValueDto simple -> simple.value;
            case AttributeMultiValueDto multi -> multi.values;
            default -> throw new IllegalStateException("Unexpected value: " + entry.getValue());
        };
    }

    private void loadRawData(Item item) {
        for (var attribute : item.getAttributes(AttributeSimpleValue.class)) {
            if (attribute.getField().getType() == Type.RAW) {
                attribute.getMetadata().forEach(metadataValue -> {
                    if (metadataValue.getDefinition().name.equals("_http_origin")) {
                        mapToRawDataDtoItem(attribute, metadataValue.getValue().toString());
                    }
                });
            }
        }
    }

    private void mapSimpleDtoValueToItem(AttributeSimpleValue attributeSimple, AttributeSimpleValueDto simpleValueDto,
            Object normalizedValue) {
        attributeSimple.setValue(normalizedValue);
        for (Map.Entry<String, Object> metaEntry : simpleValueDto.metadata.entrySet()) {
            var metadataDef = metadataService.getByName(metaEntry.getKey());
            attributeSimple.add(new MetadataValueDto(metadataDef, metaEntry.getValue()));
        }
    }

    private void mapMultiDtoValueToItem(Item item, String name, AttributeMultiValueDto multiValueDto) {
        var attributeMulti = item.getAttributeMulti(name);
        for (AttributeSimpleValueDto oneValueInMultiValueDto : multiValueDto.values) {
            var simpleAttribute = attributeMulti.addValue(oneValueInMultiValueDto.value);

            for (Map.Entry<String, Object> metaEntry : oneValueInMultiValueDto.metadata.entrySet()) {
                var metadataDef = metadataService.getByName(metaEntry.getKey());
                simpleAttribute.add(new MetadataValueDto(metadataDef, metaEntry.getValue()));
            }
        }
    }

    private void mapToRawDataDtoItem(AttributeSimpleValue attributeSimple, String mediaUrl) {
        try {
            // get media and store it into minio
            HttpURLConnection con = getMediaConnection(mediaUrl);
            InputStream body = con.getInputStream();
            String fileId = fileService.receive(body, FileType.valueOf(con.getContentType()));
            attributeSimple.setValue(fileId);
        } catch (IOException | NullPointerException e) {
            log.error(e.getMessage());
        }
    }

    private void automaticMetaProvisioning(DatasetDto dataset, Item item, LocalDateTime insertionDate) {
        var userMetadataList = userMetadataCachedService.getCurrentUserMetadataCached(tokenUtils.getToken());
        var metadataProvioningList = metaProvisioningService.getAll();

        for (MetaProvisioningReaderDto metaPro : metadataProvioningList) {
            for (UserProfileValueReadDto metaUser : userMetadataList) {
                if (metaPro.metadata().id.equals(metaUser.getUserProfile().id)) {
                    item.add(new MetadataValueDto(metaPro.metadata(), metaUser.getValue()));
                }
            }
        }
        item.add(new MetadataValueDto(MetadataSystem.ID.getMetadata(), item.getId().getAsString()));
        item.add(new MetadataValueDto(MetadataSystem.CLASS.getMetadata(), item.getoClass().getId().toString()));
        item.add(new MetadataValueDto(MetadataSystem.DATASET_VERSION.getMetadata(), item.getDatasetVersion().toString()));
        item.add(new MetadataValueDto(MetadataSystem.DATASET.getMetadata(), dataset.getId().toString()));
        item.add(new MetadataValueDto(MetadataSystem.INSERTION_DATE.getMetadata(),
                insertionDate.format(DateTimeFormatter.ISO_DATE_TIME)));
    }

    private HttpURLConnection getMediaConnection(String mediaUrl) throws IOException {
        URL url = URI.create(mediaUrl).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.disconnect();
        return con;
    }
}
