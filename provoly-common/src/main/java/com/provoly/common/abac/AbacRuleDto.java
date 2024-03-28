package com.provoly.common.abac;

import java.util.UUID;

import com.provoly.common.search.ConditionDto;

public class AbacRuleDto {
    public UUID id;
    public String name;
    public String description;
    public boolean active;
    public UUID predicate;
    public AbacRuleType type;
    public ConditionDto condition;
}
