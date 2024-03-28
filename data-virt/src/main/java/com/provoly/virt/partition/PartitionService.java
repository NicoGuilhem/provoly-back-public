package com.provoly.virt.partition;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/* *
this code come from commons-collections :
https://commons.apache.org/proper/commons-collections/apidocs/src-html/org/apache/commons/collections4/ListUtils.html
* */
@ApplicationScoped
public class PartitionService {
    public <T> List<List<T>> partition(final List<T> list, final int size) {
        if (list == null) {
            throw new NullPointerException("List must not be null");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        return new Partition<>(list, size);
    }

}