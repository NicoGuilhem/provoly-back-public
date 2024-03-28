package com.provoly.clients;

import java.util.Collection;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.item.ItemDto;
import com.provoly.common.relation.RelationsAggregateDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "data-virt")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface DataVirt {

    @POST
    @Path("/items")
    void updateItems(Collection<ItemDto> items);

    @POST
    @Path("/relations/aggregate")
    void updateAggregate(Collection<RelationsAggregateDto> relations);

}
