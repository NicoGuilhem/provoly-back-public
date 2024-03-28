package com.provoly.ref.searchrequest;

import jakarta.persistence.*;

import com.provoly.ref.model.OClass;

@Entity
@DiscriminatorValue("MONO_CLASS") // Should be same as SearchRequestType value
public class MonoClassSearchRequest extends SearchRequest {

    @ManyToOne
    private OClass oClass;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Condition condition;

    @Embedded
    private Sort sort;

    public OClass getoClass() {
        return oClass;
    }

    public void setoClass(OClass oClass) {
        this.oClass = oClass;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }
}