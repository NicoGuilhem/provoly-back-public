package com.provoly.common.transfo;

import java.util.*;

public class KosarajuAlgo<T> {

    private final Map<T, Set<T>> nodesChildGraph = new HashMap<>(); // Child to parent relations
    private final Map<T, Set<T>> nodesParentGraph = new HashMap<>(); // Parent to child relations

    private final Deque<T> l = new LinkedList<>();
    private final Set<T> visited = new HashSet<>();

    private final List<Set<T>> components = new ArrayList<>();

    public void add(T node) {
        nodesChildGraph.computeIfAbsent(node, c -> new HashSet<>());
        nodesParentGraph.computeIfAbsent(node, c -> new HashSet<>());
    }

    public void addLink(T childNode, T node) {
        nodesChildGraph.get(childNode).add(node);
        nodesParentGraph.get(node).add(childNode);
    }

    public List<Set<T>> process() {
        int childSize = nodesChildGraph.size();
        int parentSize = nodesParentGraph.size();
        if (childSize != parentSize) {
            throw new IllegalArgumentException("Graphs have not the same number of vertices "
                    + "child=" + childSize + " parent=" + parentSize);
        }

        // First pass
        for (T childNode : nodesChildGraph.keySet()) {
            visit(childNode);
        }

        // Second pass
        for (T node : l) {
            assign(node, node);
        }

        return components;
    }

    private void visit(T node) {
        if (visited.add(node)) {
            for (T parent : nodesChildGraph.get(node)) {
                visit(parent);
            }
            l.push(node);
        }
    }

    private void assign(T node, T rootNode) {

        if (!isAssign(node)) {
            getComponent(rootNode).add(node);
            for (T child : nodesParentGraph.get(node)) {
                assign(child, rootNode);
            }
        }
    }

    private boolean isAssign(T node) {
        return components.stream().anyMatch(nodes -> nodes.contains(node));
    }

    private Set<T> getComponent(T node) {
        return components.stream()
                .filter(nodes -> nodes.contains(node))
                .findFirst()
                .orElseGet(() -> {
                    Set<T> newComponent = new HashSet<>();
                    components.add(newComponent);
                    return newComponent;
                });
    }

}
