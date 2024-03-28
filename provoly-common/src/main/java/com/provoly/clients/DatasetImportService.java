package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.item.ItemDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/datasets")
@RegisterRestClient(configKey = "data-virt")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface DatasetImportService {

    @POST
    @Path("/id/{datasetId}/dataset-versions/id/{id}")
    void importData(UUID datasetId, UUID id, Collection<ItemDto> items);

}
