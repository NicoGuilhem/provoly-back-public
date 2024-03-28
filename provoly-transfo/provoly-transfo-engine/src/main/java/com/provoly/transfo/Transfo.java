package com.provoly.transfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.EntityId;
import com.provoly.common.Default;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
public class Transfo extends EntityId {
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Collection<Node> nodes = new ArrayList<>();

    @ElementCollection
    @LazyCollection(LazyCollectionOption.TRUE)
    private Collection<Link> links = new ArrayList<>();

    private UUID jobInstanceId;

    private String title;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant creationDate;

    private boolean active = false;

    public Transfo(UUID id, UUID jobInstanceId, boolean active) {
        super(id);
        this.jobInstanceId = jobInstanceId;
        this.active = active;
    }

    @Default
    public Transfo(UUID id) {
        super(id);
    }

    public String toString() {
        return "TransfoDto{" +
                "id=" + id +
                ", jobInstanceId=" + jobInstanceId +
                ", nodes=" + nodes +
                ", links=" + links +
                ", title=" + title +
                ", description=" + description +
                ", creationDate=" + creationDate +
                '}';
    }

    public Transfo() {
        super();
    }

    public Collection<Link> getLinks() {
        return links;
    }

    public void setLinks(Collection<Link> links) {
        this.links = links;
    }

    public Collection<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setJobInstanceId(UUID jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public UUID getJobInstanceId() {
        return jobInstanceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
