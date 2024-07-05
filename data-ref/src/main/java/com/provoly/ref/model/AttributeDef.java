package com.provoly.ref.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntitySlug;
import com.provoly.ref.model.field.Field;

import org.hibernate.annotations.Immutable;

@Entity
public class AttributeDef extends EntitySlug {
    @ManyToOne
    @Immutable
    private OClass oclass;

    @ManyToOne
    private Field field;

    private boolean multiValued;

    private String technicalName;

    protected AttributeDef() {
        super();
    }

    @Default
    public AttributeDef(UUID id) {
        super(id);
    }

    @Override
    public String toString() {
        var prefix = multiValued ? "(m)" : "(s)";
        return "AttributeDef{" + prefix + "'" + name + "' in '" + oclass.getName() + "'} ";
    }

    public Type getType() {
        return getField().getType();
    }

    public OClass getOclass() {
        return oclass;
    }

    public void setClass(OClass oclass) {
        this.oclass = oclass;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public String getSlugField() {
        return field.getSlug();
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }
}
