package com.provoly.ref.searchrequest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.search.ConditionType;

@Entity
@DiscriminatorValue("TRUE") // Should be same as ConditionType value
public class TrueCondition extends Condition {

    public TrueCondition() {
        this.type = ConditionType.TRUE;
    }

}
