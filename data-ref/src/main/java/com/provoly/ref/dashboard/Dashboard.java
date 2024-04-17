package com.provoly.ref.dashboard;

import java.time.Instant;
import java.util.*;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.user.ProvolyUser;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class Dashboard extends EntityNamed {
    private String description;

    private String image;

    @ManyToOne
    private ProvolyUser user;

    @CreationTimestamp
    private Instant creationDate;

    private Instant modificationDate;

    private boolean cover = false;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> manifest = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Collection<UUID> datasource = new ArrayList<>();

    private String additionalInformation;

    protected Dashboard() {
        super();
    }

    @Default
    public Dashboard(UUID id, String name, String additionalInformation) {
        super(id, name);
        this.additionalInformation = additionalInformation;
    }

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Instant modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean getCover() {
        return cover;
    }

    public void setCover(boolean cover) {
        this.cover = cover;
    }

    @SuppressWarnings("JpaAttributeTypeInspection")
    public Map<String, Object> getManifest() {
        return manifest;
    }

    public void setManifest(Map<String, Object> manifest) {
        this.manifest = manifest;
    }

    public Collection<UUID> getDatasource() {
        return datasource;
    }

    public void setDatasource(Collection<UUID> datasource) {
        this.datasource = datasource;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}