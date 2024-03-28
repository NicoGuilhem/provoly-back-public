package com.provoly.virt.partition;

import java.util.AbstractList;
import java.util.List;

/* *
this code come from commons-collections :
https://commons.apache.org/proper/commons-collections/apidocs/src-html/org/apache/commons/collections4/ListUtils.html
* */
public class Partition<T> extends AbstractList<List<T>> {
    private final List<T> list;
    private final int size;

    Partition(final List<T> list, final int size) {
        this.list = list;
        this.size = size;
    }

    @Override
    public List<T> get(final int index) {
        final int listSize = size();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " must not be negative");
        }
        if (index >= listSize) {
            throw new IndexOutOfBoundsException("Index " + index + " must be less than size " +
                    listSize);
        }
        final int start = index * size;
        final int end = Math.min(start + size, list.size());
        return list.subList(start, end);
    }

    @Override
    public int size() {
        return (int) Math.ceil((double) list.size() / (double) size);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}