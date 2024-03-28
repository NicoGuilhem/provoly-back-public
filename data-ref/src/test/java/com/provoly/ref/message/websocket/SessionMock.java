package com.provoly.ref.message.websocket;

import java.io.IOException;
import java.net.URI;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;

@ApplicationScoped
public class SessionMock implements Session {

    private Map<String, Object> userProperties = new HashMap<>();

    @Override
    public WebSocketContainer getContainer() {
        return null;
    }

    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {

    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {

    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {

    }

    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return null;
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {

    }

    @Override
    public String getProtocolVersion() {
        return null;
    }

    @Override
    public String getNegotiatedSubprotocol() {
        return null;
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public long getMaxIdleTimeout() {
        return 0;
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {

    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {

    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return 0;
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {

    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return 0;
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return new RemoteEndpointMock();
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void close(CloseReason closeReason) throws IOException {

    }

    @Override
    public URI getRequestURI() {
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public Map<String, String> getPathParameters() {
        return null;
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }

    @Override
    public Principal getUserPrincipal() {
        return new UserPrincipal() {
            @Override
            public String getName() {
                return "name";
            }
        };
    }

    @Override
    public Set<Session> getOpenSessions() {
        return null;
    }
}
