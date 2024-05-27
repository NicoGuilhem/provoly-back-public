package com.provoly.ref.widget;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Size;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.user.ProvolyUser;

import org.hibernate.annotations.CreationTimestamp;

@Entity
public class WidgetCatalog extends EntityNamed {
    /**
     * The max size of content is 100Ko.
     */
    private static final int CONTENT_MAX_SIZE = 100000;

    private String description;
    private String image;

    @ManyToOne
    private ProvolyUser user;

    @Size(max = CONTENT_MAX_SIZE)
    private String content;

    private boolean cover = false;

    @ElementCollection
    private Collection<UUID> datasource = new ArrayList<>();

    @CreationTimestamp
    private Instant creationDate;

    private Instant modificationDate;

    public WidgetCatalog() {
        super();
    }

    @Default
    public WidgetCatalog(UUID id) {
        super(id);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Collection<UUID> getDatasource() {
        return datasource;
    }

    public void setDatasource(Collection<UUID> datasource) {
        this.datasource = datasource;
    }

    public boolean isCover() {
        return cover;
    }

    public void setCover(boolean cover) {
        this.cover = cover;
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

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }
}