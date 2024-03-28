package com.provoly.ref.widget;

import java.time.Instant;
import java.util.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import com.provoly.common.Default;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityShared;
import com.provoly.ref.user.ProvolyUser;

import org.hibernate.annotations.CreationTimestamp;

@Entity
public class WidgetCatalog extends EntityShared {
    /**
     * The max size of content is 100Ko.
     */
    private static final int CONTENT_MAX_SIZE = 100000;

    private String description;
    private String image;

    @OneToMany(mappedBy = "widgetCatalog", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "user")
    private Map<ProvolyUser, ProvolyUserWidgetCatalog> belongTo = new HashMap<>();

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

    public void add(ProvolyUser user, boolean isOwner) {
        belongTo.putIfAbsent(user, new ProvolyUserWidgetCatalog(user, this, isOwner));
    }

    public void remove(ProvolyUser user) {
        belongTo.remove(user);
    }

    public ProvolyUserWidgetCatalog getForUser(ProvolyUser user) {
        if (isPublic()) {
            add(user, false);
        }
        var userWidgetCatalog = belongTo.get(user);
        if (userWidgetCatalog == null) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Widget %s/%s doesn't belong to user %s"
                    .formatted(this.id, this.name, user.getId()));
        }
        return userWidgetCatalog;
    }

    public boolean isOwner(ProvolyUser user) {
        if (belongTo.get(user) != null) {
            return belongTo.get(user).isOwner();
        }
        return false;
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

}