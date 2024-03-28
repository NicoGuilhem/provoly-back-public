package com.provoly.test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.ItemsService;
import com.provoly.common.item.CountDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.search.Direction;
import com.provoly.common.search.SortType;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Multi;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Mock
@RestClient
@ApplicationScoped
public class ItemsServiceMock implements ItemsService {

    @Inject
    DatasetFactory datasets;

    @Override
    public ItemsSearchResultDto getItems(UUID dataSourceId) {
        if (dataSourceId.equals(DatasetFactory.BIKE_STATION_DATASOURCE_ID)) {
            var items = datasets.getBikeStations().stream()
                    .map(this::convertToItem)
                    .toList();
            return new ItemsSearchResultDto(
                    Map.of(dataSourceId, items),
                    List.of(),
                    Map.of(dataSourceId, new CountDto(items.size(), true)),
                    "");
        }
        throw new IllegalStateException("Unknown dataset " + dataSourceId);
    }

    @Override
    public Multi<ItemsSearchResultDto> getPaginateItems(UUID dataSourceId, UUID id, Direction direction, SortType type) {
        return Multi.createFrom().items(getItems(dataSourceId));
    }

    private ItemDto convertToItem(BikeStation station) {
        var item = new ItemDto(UUID.randomUUID(), station.name() + "@" + UUID.randomUUID()); // TODO : Fix dataset and classId ??
        item.put("name", station.name());
        item.put("freeSpace", station.freeSpace());
        item.put("totalSpace", station.totalSpace());
        return item;
    }
}
