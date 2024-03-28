package com.provoly.transfo.engine;

import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASET;
import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASOURCE_ID;
import static com.provoly.test.DatasetFactory.BIKE_STATION_NB_ATTRIBUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.model.Type;
import com.provoly.common.search.Operator;
import com.provoly.common.transfo.*;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransfoValidTest {

    @Inject
    TransfoController controller;

    @Test
    public void emptyTransfo_noError() {
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(), "Titre");
        var result = controller.saveAndValid(transfo);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void whenLoop_globalError() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var filterOutOfLoop = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilterOut = new NodeDto(filterOutOfLoop);
        var link1 = new LinkDto(nodeInput.getId(), 0, nodeFilter.getId(), 0);
        var link2 = new LinkDto(nodeFilter.getId(), 0, nodeInput.getId(), 0);
        var link3 = new LinkDto(nodeFilter.getId(), 0, nodeFilterOut.getId(), 0);
        var transfo = new TransfoDto(UUID.randomUUID(), Set.of(nodeInput, nodeFilter, nodeFilterOut),
                Set.of(link1, link2, link3), "Titre");
        var result = controller.saveAndValid(transfo);

        assertThat(result.getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoErrorLoopInTask.class))
                .extracting(TransfoErrorLoopInTask::getIds)
                .asList()
                .containsExactlyInAnyOrder(nodeInput.getId(), nodeFilter.getId());
        assertThat(result.getStatus()).isEmpty();
    }

    @Test
    public void whenMissingNode_globalError() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var linkToNothing = new LinkDto(nodeInput.getId(), 0, nodeFilter.getId(), 0);
        UUID missingIdNode = UUID.randomUUID();
        var correctLink = new LinkDto(nodeInput.getId(), 0, missingIdNode, 0);
        var transfo = new TransfoDto(UUID.randomUUID(), Set.of(nodeInput, nodeFilter), Set.of(linkToNothing, correctLink),
                "Titre");
        var result = controller.saveAndValid(transfo);

        assertThat(result.getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoErrorMissingNode.class))
                .extracting(err -> err.getLink().getEnd().getId())
                .isEqualTo(missingIdNode);
        assertThat(result.getNodeStatus(nodeFilter.getId()).getErrors()).isEmpty();
    }

    @Test
    public void inputDataSource_missingProperty_datasetId_nodeError() {
        var input = new InputDatasource(null);
        var nodeInput = new NodeDto(input);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput), "Titre");
        var result = controller.saveAndValid(transfo);
        // Node error
        assertThat(result.getNodeStatus(nodeInput.getId()).getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoNodeErrorMissingProperty.class))
                .extracting(TransfoNodeErrorMissingProperty::getPropertyName)
                .isEqualTo("datasetId");
    }

    @Test
    public void filter_noError_andOutModel() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeFilter), "Titre");
        var result = controller.saveAndValid(transfo);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getNodeStatus(nodeInput.getId()).getErrors()).isEmpty();
        var dsOutModel = result.getNodeStatus(nodeInput.getId()).getOutModel();
        assertThat(dsOutModel).isNotNull();
        assertThat(dsOutModel.getAttributes()).hasSize(BIKE_STATION_NB_ATTRIBUTES);
        assertThat(dsOutModel.getAttributeType("name")).isEqualTo(Type.KEYWORD);
        assertThat(dsOutModel.getAttributeType("totalSpace")).isEqualTo(Type.INTEGER);
        assertThat(dsOutModel.getAttributeType("freeSpace")).isEqualTo(Type.INTEGER);
    }

    /** Filter is not linked to inputDataSource. It have no previous node */
    @Test
    public void filter_noInput_nodeError() {
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeFilter), "Titre");
        var result = controller.saveAndValid(transfo);
        // No global error
        assertThat(result.getErrors()).isEmpty();

        // Node error
        assertThat(result.getNodeStatus(nodeFilter.getId()).getErrors())
                .hasSize(1)
                .first()
                .isOfAnyClassIn(TransfoNodeErrorNoInput.class);

        // Model is null
        var dsOutModel = result.getNodeStatus(nodeFilter.getId()).getOutModel();
        assertThat(dsOutModel).isNull();

    }

    @Test
    public void filter_badAttributeName_nodeError() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        String MISSING_FIELD_NAME = "dummy";
        var filter = new Filter(MISSING_FIELD_NAME, Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeFilter), "Titre");
        var result = controller.saveAndValid(transfo);
        // Node error
        assertThat(result.getNodeStatus(nodeFilter.getId()).getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoNodeErrorMissingAttribute.class))
                .extracting(TransfoNodeErrorMissingAttribute::getAttributeName)
                .isEqualTo(MISSING_FIELD_NAME);
    }

    @Test
    public void filter_noProperty_attributeName_nodeError() {
        testFilterPropertyMissing(null, Operator.GREATER_THAN, 5, "attributeName");

    }

    @Test
    public void filter_noProperty_Operator_nodeError() {
        testFilterPropertyMissing("freeSpace", null, 5, "operator");
    }

    @Test
    public void filter_noProperty_Value_nodeError() {
        testFilterPropertyMissing("freeSpace", Operator.GREATER_THAN, null, "value");
    }

    private void testFilterPropertyMissing(String attributeName, Operator operator, Object value, String propertyName) {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter(attributeName, operator, value);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeFilter), "Titre");
        var result = controller.saveAndValid(transfo);
        // Node error
        TransfoNodeStatus nodeStatus = result.getNodeStatus(nodeFilter.getId());
        assertThat(nodeStatus.getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoNodeErrorMissingProperty.class))
                .extracting(TransfoNodeErrorMissingProperty::getPropertyName)
                .isEqualTo(propertyName);
        // OutModel is provided as a missing property for filter not blocking downstream nodes
        assertThat(nodeStatus.getOutModel()).isNotNull();
    }

    @Test
    public void filter_badType_nodeError() {
        String BAD_TYPE_FIELD_NAME = "name";
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter(BAD_TYPE_FIELD_NAME, Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeFilter), "Titre");
        var result = controller.saveAndValid(transfo);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getNodeStatus(nodeFilter.getId()).getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoNodeErrorBadType.class))
                .extracting(TransfoNodeErrorBadType::getAttributeName)
                .isEqualTo(BAD_TYPE_FIELD_NAME);
    }

    @Test
    public void outputDataset_noProperty_dataset_nodeError() {
        var input = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        var nodeInput = new NodeDto(input);
        var filter = new Filter("freeSpace", Operator.GREATER_THAN, 5);
        var nodeFilter = new NodeDto(filter);
        var output = new OutputDataset(null);
        var nodeOutput = new NodeDto(output);
        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeFilter, nodeOutput), "Titre");
        var result = controller.saveAndValid(transfo);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getNodeStatus(nodeOutput.getId()).getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoNodeErrorMissingProperty.class))
                .extracting(TransfoNodeErrorMissingProperty::getPropertyName)
                .isEqualTo("dataset");
    }

    @Test
    public void inputDataSource_outputDatasource_sameDataset() {
        var datasetId = BIKE_STATION_DATASET;
        var input = new InputDatasource(datasetId);
        var nodeInput = new NodeDto(input);

        var output = new OutputDataset(datasetId);
        var nodeOutput = new NodeDto(output);

        var transfo = TransfoDto.withLinkGeneration(UUID.randomUUID(), List.of(nodeInput, nodeOutput), "Titre");
        var result = controller.saveAndValid(transfo);
        // Node error
        assertThat(result.getErrors())
                .hasSize(1)
                .first()
                .asInstanceOf(type(TransfoErrorDatasetConflict.class))
                .extracting(TransfoErrorDatasetConflict::getDatasetIds)
                .isEqualTo(Set.of(datasetId));
    }

}
