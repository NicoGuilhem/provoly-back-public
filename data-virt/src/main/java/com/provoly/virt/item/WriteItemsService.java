package com.provoly.virt.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetService;
import com.provoly.clients.ModelService;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.ItemDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.Item;
import com.provoly.virt.partition.PartitionService;
import com.provoly.virt.storage.InsertionError;
import com.provoly.virt.storage.StorageWriteAdapter;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WriteItemsService {

    private Logger log;

    private ModelService modelService;

    private DatasetService datasetService;

    private StorageWriteAdapter storageItemService;

    private PartitionService partitionService;

    private DataVirtProperties dataVirtProperties;

    private ItemTransformer itemTransformer;

    private ItemsNotifier itemsNotifier;

    public WriteItemsService(Logger log,
            @RestClient ModelService modelService,
            @RestClient DatasetService datasetService,
            StorageWriteAdapter storageItemService,
            PartitionService partitionService,
            DataVirtProperties dataVirtProperties,
            ItemTransformer itemTransformer,
            ItemsNotifier itemsNotifier) {
        this.log = log;
        this.modelService = modelService;
        this.datasetService = datasetService;
        this.storageItemService = storageItemService;
        this.partitionService = partitionService;
        this.dataVirtProperties = dataVirtProperties;
        this.itemTransformer = itemTransformer;
        this.itemsNotifier = itemsNotifier;
    }

    public List<InsertionError> addItems(Collection<Item> items) {
        if (items.stream().map(Item::getDatasetVersion).collect(Collectors.groupingBy(uuid -> uuid)).size() > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Items list is provided with multiple dataset versions");
        }

        List<InsertionError> errors = storageItemService.add(items);

        if (errors.isEmpty() && dataVirtProperties.notification()) {
            // Send items to notification service
            // TODO rework data-link, issue https://gitlab.groupeonepoint.com/cds-bdx/pole-edition/yap/yap-back/-/issues/431
            itemsNotifier.notifyItemWrittenToStorage(items);
        }

        return errors;
    }

    // TODO : This method should not exists. ItemDto is only for interface, it should not be used in the backend
    public List<InsertionError> addItemsDto(Collection<ItemDto> itemsDto) {
        log.debug("Start adding items");

        if (itemsDto.isEmpty()) {
            log.debug("Empty list of items, returning empty errors");
            return new ArrayList<>();
        }

        if (itemsDto.stream().map(ItemDto::getDatasetVersionId).collect(Collectors.toSet()).size() > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Items list is provided with multiple dataset versions");
        }

        ItemDto anyItem = itemsDto.stream().findAny().get(); // Items presences checked above
        DatasetDto datasetDto = datasetService.searchByDatasetVersionId(UUID.fromString(anyItem.getDatasetVersionId()));

        if (itemsDto.stream().anyMatch(itemDto -> !itemDto.getoClass().equals(datasetDto.getoClass()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "All items must have same OclassId thant Dataset OclassId : %s".formatted(datasetDto.getoClass()));
        }

        // Convert itemDto to item and send them to Elastic
        List<InsertionError> errors = partitionService.partition(itemsDto.stream().toList(), dataVirtProperties.chunkSize())
                .stream()
                .map(itemDtoChunk -> itemTransformer.transform(datasetDto, itemDtoChunk))
                .map(this::addItems)
                .flatMap(List::stream)
                .toList();

        if (!errors.isEmpty()) {
            log.infof("storage insertion error : %s", errors);
        }
        return errors;
    }
}