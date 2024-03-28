package com.provoly.ref.message;

public interface MessageListener {

    void onMessage(Message message);

    void onConnection(String id);
}