package com.provoly.virt.metadata;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.MetadataRefService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.MetadataValueDto;
import com.provoly.virt.item.ReadItemsService;
import com.provoly.virt.item.WriteItemsService;
import com.provoly.virt.storage.InsertionError;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class MetadataService {

    @Inject
    @RestClient
    MetadataRefService metadataRefService;

    @Inject
    WriteItemsService itemsService;

    @Inject
    ReadItemsService getItemsService;

    public void addMetadataToItem(ItemId itemId, String metadataName, String value) {
        addMetadata(itemId, metadataName, (item, metadataDef) -> item.add(new MetadataValueDto(metadataDef, value)));
    }

    public void addMetadataToAttribute(ItemId itemId, String attributeName, String metadataName, Object value) {
        addMetadata(itemId, metadataName, (item, metadataDef) -> {
            var attributeValue = item.getAttributeSimple(attributeName);
            attributeValue.add(new MetadataValueDto(metadataDef, value));
        });
    }

    private void addMetadata(ItemId itemId, String metadataName, BiConsumer<Item, MetadataDefDto> doAdd) {
        var metadataDef = metadataRefService.getByName(metadataName);
        if (metadataDef == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown metadata " + metadataName);
        }

        var item = getItemsService.get(itemId);
        doAdd.accept(item, metadataDef);
        List<InsertionError> errors = itemsService.addOrUpdateItems(Collections.singleton(item), ItemUpdateMode.REPLACE);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Add items fail : " + item);
        }
    }
}
