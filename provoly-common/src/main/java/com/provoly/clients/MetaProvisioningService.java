package com.provoly.clients;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.metadata.MetaProvisioningReaderDto;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/meta-provisioning")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface MetaProvisioningService {

    @GET
    @CacheResult(cacheName = "all-meta-provisioning")
    List<MetaProvisioningReaderDto> getAll();
}
