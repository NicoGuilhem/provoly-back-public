package com.provoly.ref.searchrequest;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.search.SearchRequestType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
//@DiscriminatorColumn(name = "type") // This annotation add a WARN : https://hibernate.atlassian.net/browse/HHH-6911
public class SearchRequest {

    @Id
    @GeneratedValue
    public UUID id;

    //@Column(insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    public SearchRequestType type;

    public FullSearchCondition fullSearch;

}
