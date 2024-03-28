package com.provoly.ref.abac;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.ref.abac.predicate.Predicate;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.model.OClass;
import com.provoly.ref.searchrequest.Condition;

import org.hibernate.annotations.Immutable;

@Entity
public class AbacRule extends EntityNamed {
    private String description;
    private boolean active;

    @ManyToOne
    private Predicate predicate;

    @Enumerated(EnumType.STRING)
    AbacRuleType type;

    @ManyToOne
    @Immutable
    private OClass oClass;

    @ManyToOne(cascade = CascadeType.ALL)
    private Condition condition;

    protected AbacRule() {
        super();
    }

    @Default
    public AbacRule(UUID id) {
        super(id);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public AbacRuleType getType() {
        return type;
    }

    public void setType(AbacRuleType type) {
        this.type = type;
    }

    public OClass getoClass() {
        return oClass;
    }

    public void setoClass(OClass oClass) {
        this.oClass = oClass;
    }
}
