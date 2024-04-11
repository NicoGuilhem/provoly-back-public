package com.provoly.virt.storage.elasticbased.elastic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.provoly.clients.MetadataRefService;
import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.storage.elasticbased.ElasticSupport;

import org.elasticsearch.client.ResponseBuilder;
import org.elasticsearch.client.ResponseException;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;

public class ElasticWriteServiceTest {
    ElasticWriteService elasticStorageWriteAdapter;
    MetadataRefService metadataService;
    ElasticLayout supportMock;
    ElasticsearchClient elasticsearchClientMock;
    ElasticSupport elasticSupport;
    static Logger logger = Logger.getLogger(ElasticWriteService.class);

    @BeforeEach
    public void init() {
        supportMock = Mockito.mock(ElasticLayout.class);
        elasticsearchClientMock = Mockito.mock(ElasticsearchClient.class);
        elasticStorageWriteAdapter = new ElasticWriteService(elasticsearchClientMock, metadataService,
                supportMock, elasticSupport, logger);
    }

    @Test
    void test_addItems_with_too_large_throw_illegal() throws IOException {

        // 1st try direct mock but final class and package visible
        when(elasticsearchClientMock.bulk(Mockito.any(BulkRequest.class))).thenThrow(
                new ResponseException(ResponseBuilder.build()));

        List<Item> items = new ArrayList<>();
        OClassDetailsDto oClassDto = new OClassDetailsDto(UUID.randomUUID(), "name", "", "", List.of(), Storage.ELASTIC, List.of());
        UUID datasetId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            items.add(new Item(new ItemId(datasetId, UUID.randomUUID().toString()), oClassDto, Collections.emptyList()));
        }
        Assert.assertThrows(IllegalStateException.class, () -> elasticStorageWriteAdapter.add(items));
    }

    @Test
    void test_addItems_with_too_large_throw_timeout() throws IOException {

        // 1st try direct mock but final class and package visible
        when(elasticsearchClientMock.bulk(Mockito.any(BulkRequest.class))).thenThrow(
                new SocketTimeoutException("timeout"));

        List<Item> items = new ArrayList<>();
        OClassDetailsDto oClassDto = new OClassDetailsDto(UUID.randomUUID(), "name", "", "", List.of(), Storage.ELASTIC, List.of());
        UUID datasetId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            items.add(new Item(new ItemId(datasetId, UUID.randomUUID().toString()), oClassDto, Collections.emptyList()));
        }
        Assert.assertThrows(BusinessException.class, () -> elasticStorageWriteAdapter.add(items));
    }

    @Test
    void test_addItems_with_nothing_return_empty_list() throws IOException {
        when(elasticsearchClientMock.bulk(Mockito.any(BulkRequest.class))).thenThrow(
                new ResponseException(ResponseBuilder.build()));

        List<Item> items = new ArrayList<>();
        assertTrue(elasticStorageWriteAdapter.add(items).isEmpty());
    }

    @Test
    void test_addItems_success_with_one_item_error_should_return_one_error() throws IOException {
        OClassDetailsDto oClassDto = new OClassDetailsDto(UUID.randomUUID(), "name", "", "", List.of(), Storage.ELASTIC,
                List.of());
        UUID datasetId = UUID.randomUUID();
        ArgumentMatcher<BulkRequest> bulkMatchThrow = request -> request.operations().size() > 2;
        ArgumentMatcher<BulkRequest> bulkMatchNotThrow = request -> request.operations().size() == 2;
        ArgumentMatcher<BulkRequest> bulkMatchNotThrowWithOneError = request -> request.operations().size() == 1;
        List<BulkResponseItem> returnTwoItems = new ArrayList<>();
        returnTwoItems
                .add(new BulkResponseItem.Builder().operationType(OperationType.Update).index("indexes").status(0).build());
        returnTwoItems
                .add(new BulkResponseItem.Builder().operationType(OperationType.Update).index("indexes").status(0).build());

        List<BulkResponseItem> returnOneItemWithError = new ArrayList<>();
        returnOneItemWithError.add(new BulkResponseItem.Builder().operationType(OperationType.Update)
                .index("indexes")
                .status(1)
                .error(new ErrorCause.Builder()
                        .reason("error")
                        .causedBy(new ErrorCause.Builder().reason("error").build()).build())
                .build());

        doThrow(new ResponseException(ResponseBuilder.build())).when(elasticsearchClientMock)
                .bulk(Mockito.argThat(bulkMatchThrow));
        doReturn(new BulkResponse.Builder().errors(false).items(returnTwoItems).took(123L).build())
                .when(elasticsearchClientMock).bulk(Mockito.argThat(bulkMatchNotThrow));
        doReturn(new BulkResponse.Builder().errors(true).items(returnOneItemWithError).took(123L).build())
                .when(elasticsearchClientMock).bulk(Mockito.argThat(bulkMatchNotThrowWithOneError));

        List<Item> items = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            items.add(new Item(new ItemId(datasetId, UUID.randomUUID().toString()), oClassDto, Collections.emptyList()));
        }
        assertEquals(1, elasticStorageWriteAdapter.add(items).size());
    }

    @Test
    void test_addItems_success_with_no_error_should_return_empty_list() throws IOException {
        OClassDetailsDto oClassDto = new OClassDetailsDto(UUID.randomUUID(), "name", "", "", List.of(), Storage.ELASTIC,
                List.of());
        UUID datasetId = UUID.randomUUID();
        ArgumentMatcher<BulkRequest> bulkMatchThrow = request -> request.operations().size() > 2;
        ArgumentMatcher<BulkRequest> bulkMatchNotThrow = request -> request.operations().size() <= 2;

        List<BulkResponseItem> returnItems = new ArrayList<>();
        returnItems.add(new BulkResponseItem.Builder().operationType(OperationType.Update).index("indexes").status(0).build());
        returnItems.add(new BulkResponseItem.Builder().operationType(OperationType.Update).index("indexes").status(0).build());

        doThrow(new ResponseException(ResponseBuilder.build())).when(elasticsearchClientMock)
                .bulk(Mockito.argThat(bulkMatchThrow));
        doReturn(new BulkResponse.Builder().errors(false).items(returnItems).took(123L).build()).when(elasticsearchClientMock)
                .bulk(Mockito.argThat(bulkMatchNotThrow));

        List<Item> items = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            items.add(new Item(new ItemId(datasetId, UUID.randomUUID().toString()), oClassDto, Collections.emptyList()));
        }
        assertTrue(elasticStorageWriteAdapter.add(items).isEmpty());
    }
}
