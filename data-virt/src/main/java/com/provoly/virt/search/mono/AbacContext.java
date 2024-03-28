package com.provoly.virt.search.mono;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;

import com.provoly.security.CurrentSubjectProvider;
import com.provoly.virt.metadata.UserMetadataCachedService;

import io.vertx.core.http.HttpServerRequest;

@ApplicationScoped
public class AbacContext {
    private HttpServerRequest request;
    private UserMetadataCachedService userService;
    private CurrentSubjectProvider currentSubjectProvider;

    public AbacContext(@Context HttpServerRequest request,
            UserMetadataCachedService userService,
            CurrentSubjectProvider currentSubjectProvider) {
        this.request = request;
        this.userService = userService;
        this.currentSubjectProvider = currentSubjectProvider;
    }

    public User getUser() {
        var metadata = userService.getCurrentUserMetadataCached(currentSubjectProvider.getSub());
        return new User(currentSubjectProvider.getName(), currentSubjectProvider.getRoles(), metadata);
    }

    public ProvolyRequest getRequest() {
        return new ProvolyRequest(request);
    }

}
