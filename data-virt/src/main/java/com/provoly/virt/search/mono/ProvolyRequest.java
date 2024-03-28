package com.provoly.virt.search.mono;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

@RegisterForReflection
public class ProvolyRequest {

    private final HttpServerRequest request;

    public ProvolyRequest(HttpServerRequest request) {
        this.request = request;
    }

    public String header(String name) {
        return request.headers().get(name);
    }

    public SocketAddress getRemoteAddress() {
        return request.remoteAddress();
    }
}
