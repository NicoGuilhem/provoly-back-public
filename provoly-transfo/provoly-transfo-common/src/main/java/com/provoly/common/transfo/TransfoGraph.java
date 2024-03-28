package com.provoly.common.transfo;

import java.util.*;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class TransfoGraph {
    private final TransfoDto transfo;

    private final KosarajuAlgo<NodeDto> nodeSorter = new KosarajuAlgo<>();

    Map<NodeDto, NodeDto> previousNodeMap = new HashMap<>();
    Map<UUID, NodeDto> nodes = new HashMap<>();

    public TransfoGraph(TransfoDto dto) {
        this.transfo = dto;
        dto.getNodes().forEach(this::addNode);
        dto.getLinks().forEach(this::addLink);
    }

    public List<NodeDto> ordered() {

        var orderedNode = new ArrayList<NodeDto>();
        for (Set<NodeDto> nodesGroupe : nodeSorter.process()) {
            if (nodesGroupe.size() != 1)
                throw new BusinessException(ErrorCode.TECHNICAL, "Graph error " + transfo);
            orderedNode.add(nodesGroupe.stream().findFirst().get());
        }

        return orderedNode;
    }

    public NodeDto getPrevious(NodeDto node) {
        return previousNodeMap.get(node);
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
            throw new BusinessException(ErrorCode.TECHNICAL, "Graph error " + transfo);
        }
        var endNode = nodes.get(link.getEnd().getId());
        if (endNode == null) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Graph error " + transfo);
        }
        nodeSorter.addLink(startNode, endNode);
        previousNodeMap.put(endNode, startNode);
    }

}
