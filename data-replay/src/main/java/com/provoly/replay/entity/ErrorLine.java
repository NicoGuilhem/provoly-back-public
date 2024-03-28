package com.provoly.replay.entity;

public class ErrorLine {

    private String topic;
    private String codeError;
    private boolean reset;

    public ErrorLine(String topic, String codeError, boolean reset) {
        this.topic = topic;
        this.codeError = codeError;
        this.reset = reset;
    }

    public String getTopic() {
        return topic;
    }

    public String getCodeError() {
        return codeError;
    }

    public boolean isReset() {
        return reset;
    }
}
