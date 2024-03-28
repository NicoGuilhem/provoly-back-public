package com.provoly.common.relation;

import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

public class RelationsAggregateDto {

    public UUID link;
    public String aggregateId;
    public final List<String> source;
    public final List<String> dest;

    public RelationsAggregateDto(UUID link, String attributeValue, List<String> source, List<String> dest) {
        this.link = link;
        this.aggregateId = DigestUtils.sha1Hex(link + attributeValue);
        this.source = source;
        this.dest = dest;
    }

    public int size() {
        return source.size() * dest.size();
    }
}
