package com.provoly.transfo.runner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Item {

    private List<String> attributes = new ArrayList<>();

    Item() {
    }

    public Item(String... attributes) {
        this.attributes.addAll(Arrays.stream(attributes).toList());
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
}
