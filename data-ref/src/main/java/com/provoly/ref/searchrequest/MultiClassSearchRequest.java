package com.provoly.ref.searchrequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.search.MultiSearchType;

@Entity
@DiscriminatorValue("MULTI_CLASS") // Should be same as SearchRequestType value
public class MultiClassSearchRequest extends SearchRequest {

    @Enumerated(EnumType.STRING)
    private MultiSearchType multiType;

    @ElementCollection
    private List<UUID> oClasses = new ArrayList<>();

    @OneToMany(mappedBy = "searchRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<FieldCondition> fields = new ArrayList<>();

    public void addField(FieldCondition condition) {
        fields.add(condition);
        condition.setSearchRequest(this);
    }

    public List<UUID> getoClasses() {
        return oClasses;
    }

    public Collection<FieldCondition> getFields() {
        return fields;
    }

    public MultiSearchType getMultiType() {
        return multiType;
    }

    public void setoClasses(List<UUID> oClasses) {
        this.oClasses = oClasses;
    }

    public void setFields(Collection<FieldCondition> fields) {
        this.fields = fields;
    }

    public void setMultiType(MultiSearchType multiType) {
        this.multiType = multiType;
    }

}
