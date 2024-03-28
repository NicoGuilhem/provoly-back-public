package com.provoly.common.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface ItemAggregationDto<K, V> {
    K getKey();

    V getValue();

    class SimpleItemDto implements ItemAggregationDto<Object, Object> {

        private Object key;
        private Object value;

        public SimpleItemDto(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    class GroupedItemDto implements ItemAggregationDto<Object, List<SimpleItemDto>> {
        private Object key;
        private List<SimpleItemDto> value;

        public GroupedItemDto(Object key, List<SimpleItemDto> value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        @JsonProperty("groupBy")
        public List<SimpleItemDto> getValue() {
            return value;
        }

    }
}
