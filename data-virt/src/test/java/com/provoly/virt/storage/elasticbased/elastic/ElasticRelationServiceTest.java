package com.provoly.virt.storage.elasticbased.elastic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.provoly.common.error.BusinessException;
import com.provoly.common.relation.RelationDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.item.ReadItemsService;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

class ElasticRelationServiceTest {

    private ElasticRelationService elasticRelationService;

    static Logger log = Logger.getLogger(ElasticRelationService.class);
    private ElasticsearchClient elasticClient;
    private ReadItemsService itemService;
    private DataVirtProperties dataVirtProperties;

    @BeforeEach
    void setUp() {
        log = Mockito.mock(Logger.class);
        elasticClient = Mockito.mock(ElasticsearchClient.class);
        itemService = Mockito.mock(ReadItemsService.class);
        dataVirtProperties = Mockito.mock(DataVirtProperties.class);
        elasticRelationService = new ElasticRelationService(log,
                elasticClient,
                itemService,
                dataVirtProperties);
    }

    @Test
    void test_getRelationsByItemAndRelation_withoutRelation_throws() {
        assertThatThrownBy(() -> elasticRelationService.getRelationsByItemAndRelation(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Relation type is required to search relations by item id and relation type.");
    }

    @Test
    void test_getRelationsByItemAndRelation_withoutRelationType_throws() {
        RelationDto relationDto = new RelationDto(null, null, null);
        assertThatThrownBy(() -> elasticRelationService.getRelationsByItemAndRelation(relationDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Relation type is required to search relations by item id and relation type.");
    }

    @Test
    void test_getRelationsByItemAndRelation_withoutSourceOrDestination_throws() {
        RelationDto relationDto = new RelationDto("relationType1", null, null);
        assertThatThrownBy(() -> elasticRelationService.getRelationsByItemAndRelation(relationDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Exactly one value amongst source or destination must be filled to search relations by item's identifier and relation's type.");
    }

    @Test
    void test_getRelationsByItemAndRelation_withSourceAndDestination_throws() {
        RelationDto relationDto = new RelationDto("relationType1", "itemIdSource", "itemIdDestination");
        assertThatThrownBy(() -> elasticRelationService.getRelationsByItemAndRelation(relationDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Exactly one value amongst source or destination must be filled to search relations by item's identifier and relation's type.");
    }
}