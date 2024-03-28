package com.provoly.ref.metadata;

import jakarta.persistence.*;

import com.provoly.common.search.Operator;
import com.provoly.ref.searchrequest.Condition;

@Entity
@DiscriminatorValue("METADATA") // Should be same as ConditionType value
public class MetadataCondition extends Condition {

    @ManyToOne
    private MetadataDef metadataDef;

    @Enumerated(EnumType.STRING)
    private Operator operator;

    private String value;

    public MetadataCondition() {
        // Only for JPA
    }

    public MetadataDef getMetadata() {
        return metadataDef;
    }

    public void setMetadata(MetadataDef metadata) {
        this.metadataDef = metadata;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
