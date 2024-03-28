package com.provoly.common.transfo;

import java.util.*;
import java.util.function.Consumer;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class TransfoDto {
    private final UUID id;
    private final Collection<NodeDto> nodes;
    private final Collection<LinkDto> links;
    private final String title;
    private final String description;

    @JsonCreator
    @Default
    public TransfoDto(UUID id, Set<NodeDto> nodes, String title, String description) {
        this(id, nodes, new HashSet<>(), title, description);
    }

    public TransfoDto(UUID id, Set<NodeDto> nodes, Set<LinkDto> links, String title) {
        this(id, nodes, links, title, null);
    }

    public TransfoDto(UUID id, Set<NodeDto> nodes, Set<LinkDto> links, String title,
            String description) {
        this.id = id;
        this.nodes = nodes;
        this.links = links;
        this.title = title;
        this.description = description;
    }

    /**
     * Convenient method inserting and linking all node in order of list
     **/
    public static TransfoDto withLinkGeneration(UUID id, List<NodeDto> nodes, String title) {
        return new TransfoDto(id, new HashSet<>(nodes), generateLinks(nodes), title);
    }

    private static Set<LinkDto> generateLinks(List<NodeDto> nodes) {
        Set<LinkDto> links = new HashSet<>();
        for (int i = 1; i < nodes.size(); i++) {
            var currentNode = nodes.get(i - 1);
            var nextNode = nodes.get(i);
            links.add(new LinkDto(currentNode.getId(), 0, nextNode.getId(), 0));
        }
        return links;
    }

    public <T extends NodeSpec> void forEach(Class<T> specClass, Consumer<T> consumer) {
        nodes.stream()
                .filter(n -> n.isType(specClass))
                .map(n -> n.specAs(specClass))
                .forEach(consumer);
    }

    @Override
    public String toString() {
        return "TransfoDto{" +
                "id=" + id +
                ", nodes=" + nodes +
                ", links=" + links +
                ", title=" + title +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Collection<NodeDto> getNodes() {
        return nodes;
    }

    public Collection<LinkDto> getLinks() {
        return links;
    }

    public String getDescription() {
        return description;
    }
}
