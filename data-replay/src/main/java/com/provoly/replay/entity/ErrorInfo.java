package com.provoly.replay.entity;

public class ErrorInfo {
    private String label;

    private int count;

    public ErrorInfo() {
    }

    public ErrorInfo(String label, int count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
