package com.provoly.ref.message.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;

public class RemoteEndpointMock implements RemoteEndpoint.Async {
    @Override
    public long getSendTimeout() {
        return 0;
    }

    @Override
    public void setSendTimeout(long timeoutmillis) {

    }

    @Override
    public void sendText(String text, SendHandler handler) {

    }

    @Override
    public Future<Void> sendText(String text) {
        return null;
    }

    @Override
    public Future<Void> sendBinary(ByteBuffer data) {
        return null;
    }

    @Override
    public void sendBinary(ByteBuffer data, SendHandler handler) {

    }

    @Override
    public Future<Void> sendObject(Object data) {
        return null;
    }

    @Override
    public void sendObject(Object data, SendHandler handler) {

    }

    @Override
    public void setBatchingAllowed(boolean allowed) throws IOException {

    }

    @Override
    public boolean getBatchingAllowed() {
        return false;
    }

    @Override
    public void flushBatch() throws IOException {

    }

    @Override
    public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }

    @Override
    public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }
}
