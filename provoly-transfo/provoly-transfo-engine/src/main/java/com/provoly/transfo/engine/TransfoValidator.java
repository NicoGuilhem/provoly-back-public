package com.provoly.transfo.engine;

import java.util.*;
import java.util.stream.Collectors;

import com.provoly.clients.DataSourceService;
import com.provoly.clients.ModelService;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.transfo.*;

// TODO : Multi slot
public class TransfoValidator {

    private final TransfoDto transfo;
    private final TransfoStatus status = new TransfoStatus();

    private final KosarajuAlgo<NodeDto> nodeSorter = new KosarajuAlgo<>();

    Map<NodeDto, NodeDto> previousNodeMap = new HashMap<>();
    Map<UUID, NodeDto> nodes = new HashMap<>();

    Map<NodeDto, TransfoNodeStatus> nodeStatusMap = new HashMap<>(); // TODO : Only one outmodel for now

    public TransfoValidator(TransfoDto dto) {
        this.transfo = dto;
        dto.getNodes().forEach(this::addNode);
        dto.getLinks().forEach(this::addLink);
    }

    public TransfoStatus validate(DataSourceService dataSourceService, ModelService modelService) {
        var conflicts = getSameDatasetDefInInputAndOutput(dataSourceService);
        if (!conflicts.isEmpty()) {
            status.addError(new TransfoErrorDatasetConflict(conflicts));
        }

        var orderedNode = new ArrayList<NodeDto>();
        for (Set<NodeDto> nodesGroupe : nodeSorter.process()) {
            if (nodesGroupe.isEmpty())
                throw new BusinessException(ErrorCode.TECHNICAL, "Kasaraju error " + transfo);
            if (nodesGroupe.size() > 1) {
                var ids = nodesGroupe.stream().map(NodeDto::getId).collect(Collectors.toList());
                status.addError(new TransfoErrorLoopInTask(ids));
                return status;
            }
            orderedNode.add(nodesGroupe.stream().findFirst().get());
        }

        // Ordered node contain a list of node where a node is always present after every node it depends on
        // We are not supporting cyclic graph.
        // get the previous validation state and validate the current node
        // Add to the nodeStatusMap the current result
        for (NodeDto node : orderedNode) {
            nodeStatusMap.computeIfAbsent(node, n -> getTransfoNodeStatus(dataSourceService, modelService, n));
        }

        status.add(nodeStatusMap.values());
        return status;
    }

    private Set<UUID> getSameDatasetDefInInputAndOutput(DataSourceService dataSourceService) {
        var inputDatasetDef = nodes.values()
                .stream()
                .filter(nodeDto -> nodeDto.isType(InputDatasource.class))
                .map(nodeDto -> ((InputDatasource) nodeDto.getSpec()).getDatasetId())
                .filter(sourceId -> {
                    if (sourceId != null) {
                        return dataSourceService.getDataSourceDetails(sourceId).type() == DataSourceType.DATASET;
                    }
                    return false;
                })
                .collect(Collectors.toSet());

        var outputDatasetDef = nodes.values()
                .stream()
                .filter(nodeDto -> nodeDto.isType(OutputDataset.class))
                .map(nodeDto -> ((OutputDataset) nodeDto.getSpec()).getDataset())
                .collect(Collectors.toSet());

        inputDatasetDef.retainAll(outputDatasetDef);
        return inputDatasetDef;
    }

    private TransfoNodeStatus getTransfoNodeStatus(DataSourceService dataSourceService, ModelService modelService,
            NodeDto node) {
        if (node.isType(InputDatasource.class)) {
            // Special case for InputDatasource because they need service. // Maybe find a better way
            var inputDataSource = node.specAs(InputDatasource.class);
            return validateInputDataSourceNode(dataSourceService, modelService, node.getId(), inputDataSource);
        } else {
            return validateNode(node);
        }
    }

    private TransfoNodeStatus validateInputDataSourceNode(DataSourceService dataSourceService, ModelService modelService,
            UUID nodeId, InputDatasource inputDataSource) {
        UUID datasetId = inputDataSource.getDatasetId();
        if (datasetId == null) {
            TransfoNodeStatus nodeStatus = new TransfoNodeStatus(nodeId);
            nodeStatus.addError(new TransfoNodeErrorMissingProperty("datasetId"));
            return nodeStatus;
        } else {
            var dataSourceDetails = dataSourceService.getDataSourceDetails(datasetId);
            // TODO : Create a getClassDetails in modelService
            var oclass = modelService.getDetails(dataSourceDetails.oClass());
            var fields = modelService.getFieldsForClass(dataSourceDetails.oClass()).stream()
                    .collect(Collectors.toMap(f -> f.id, f -> f, (fieldDto, fieldDto2) -> fieldDto));
            IntermediateModel model = new IntermediateModel();
            // Add to model every attribute of the class
            oclass.getAttributes().forEach(attr -> model.addAttribute(attr.getName(), fields.get(attr.getField().id)));
            return new TransfoNodeStatus(nodeId, model);
        }
    }

    private TransfoNodeStatus validateNode(NodeDto node) {
        var previousNode = previousNodeMap.get(node);
        var previousStatus = nodeStatusMap.get(previousNode);
        return node.validate(previousStatus);
    }

    private void addNode(NodeDto node) {
        nodeSorter.add(node);
        if (nodes.put(node.getId(), node) != null) {
            throw new TransfoException(transfo, "Duplicate node : " + node);
        }
    }

    private void addLink(LinkDto link) {
        var startNode = nodes.get(link.getStart().getId());
        if (startNode == null) {
            status.addError(new TransfoErrorMissingNode(link));
            return;
        }
        var endNode = nodes.get(link.getEnd().getId());
        if (endNode == null) {
            status.addError(new TransfoErrorMissingNode(link));
            return;
        }
        nodeSorter.addLink(startNode, endNode);
        previousNodeMap.put(endNode, startNode);
    }

}
