package com.provoly.ref.user.metadata;

import java.util.*;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.VariableType;
import com.provoly.ref.entity.EntitySlug;

@Entity
public class UserProfile extends EntitySlug {

    @Enumerated(EnumType.STRING)
    VariableType type;

    String description;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "userProfile", orphanRemoval = true)
    private Set<UserProfileAllowedValue> values = new HashSet<>();

    protected UserProfile() {
        super();
    }

    @Default
    public UserProfile(UUID id, String name, VariableType type, String description, String slug) {
        super(id);
        this.name = name;
        this.type = type;
        this.description = description;
        this.slug = slug;
    }

    public void addValue(UserProfileAllowedValue value) {
        value.setMetadataUserDef(this);
        values.add(value);
    }

    public Set<UserProfileAllowedValue> getValues() {
        return Collections.unmodifiableSet(values);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(id, that.getId());
    }

}
