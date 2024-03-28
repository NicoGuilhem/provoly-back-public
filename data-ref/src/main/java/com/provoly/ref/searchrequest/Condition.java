package com.provoly.ref.searchrequest;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.search.ConditionType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Condition {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    public ConditionType type;

    @Override
    public String toString() {
        return "Condition{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
