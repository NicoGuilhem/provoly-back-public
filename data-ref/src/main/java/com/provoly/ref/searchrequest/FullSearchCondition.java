package com.provoly.ref.searchrequest;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FullSearchCondition {

    @Column(name = "full_search_value")
    public String value;

}
