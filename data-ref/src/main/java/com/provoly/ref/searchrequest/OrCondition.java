package com.provoly.ref.searchrequest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.search.ConditionType;

@Entity
@DiscriminatorValue("OR") // Should be same as ConditionType value
public class OrCondition extends ComposedCondition {

    public OrCondition() {
        this.type = ConditionType.OR;
    }

}
