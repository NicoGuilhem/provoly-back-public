package com.provoly.virt.storage.elasticbased.elastic;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.storage.elasticbased.ElasticSupport;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ElasticSearchServiceTest {

    ElasticSearchService elasticSearchService;

    @BeforeEach
    public void init() {
        elasticSearchService = new ElasticSearchService(null, null, null, null, null, null, null, new ElasticSupport(), null);
    }

    @Test
    public void requestLimitGreaterThan10000_shouldThrowException() {

        MonoClassRequestDto requestDto = new MonoClassRequestDto(UUID.randomUUID(), new ArrayList<>(), 10001);

        Exception exception = Assert.assertThrows(BusinessException.class, () -> {
            elasticSearchService.search(null, requestDto, null);
        });

        assertEquals("Request limit can't be greater than 10000 for an Elastic based Storage", exception.getMessage());
    }
}