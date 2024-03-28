package com.provoly.ref.dataset;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import com.provoly.common.dataset.DatasetType;
import com.provoly.ref.entity.EntitySlug;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.ProvolyUser;

import org.hibernate.annotations.Immutable;

@Entity
public class Dataset extends EntitySlug {

    @ManyToOne
    @Immutable
    private OClass oClass;

    @Enumerated(EnumType.STRING)
    private DatasetType type;

    private String description;

    @ManyToOne
    private ProvolyUser user;

    protected Dataset() {
        super();
    }

    public Dataset(UUID id) {
        super(id);
    }

    public OClass getoClass() {
        return oClass;
    }

    public void setoClass(OClass oClass) {
        this.oClass = oClass;
    }

    public DatasetType getType() {
        return type;
    }

    public void setType(DatasetType type) {
        this.type = type;
    }

    public boolean canUpdateTo(Dataset dataset) {
        return isSameType().and(isSameClass()).test(dataset);
    }

    private Predicate<Dataset> isSameType() {
        return dataset -> this.type.equals(dataset.getType());
    }

    private Predicate<Dataset> isSameClass() {
        return dataset -> Objects.equals(this.oClass, dataset.oClass);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }
}
