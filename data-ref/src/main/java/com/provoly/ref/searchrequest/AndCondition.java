package com.provoly.ref.searchrequest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.search.ConditionType;

@Entity
@DiscriminatorValue("AND") // Should be same as ConditionType value
public class AndCondition extends ComposedCondition {

    public AndCondition() {
        this.type = ConditionType.AND;
    }

}
