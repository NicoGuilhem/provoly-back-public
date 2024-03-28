package com.provoly.virt.metadata;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.ProvolyUserService;
import com.provoly.common.metadata.UserProfileValueReadDto;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/*
We need to use a unique value as a cache key thus we use the sub.
*/
@ApplicationScoped
public class UserMetadataCachedService {
    @Inject
    @RestClient
    ProvolyUserService provolyUserService;

    /**
     *
     * @param sub Used as unique id to differentiate multiple users requesting there meta-data.
     *        This parameter is not usefull for the request as it's already propagated by <code>AccessTokenRequestFilter</code>
     * @return
     */
    @CacheResult(cacheName = "current-user-metadata")
    public Collection<UserProfileValueReadDto> getCurrentUserMetadataCached(String sub) {
        return provolyUserService.getCurrentUserMetadata();
    }
}
