package com.provoly.ref.groups;

import java.util.UUID;

import com.provoly.ref.user.ProvolyUser;

/**
 * Representation of entities with grant restrictions based on a user (the owner).
 *
 * @see GrantService
 */
public interface WithGrantRestrictions {

    public UUID getId();

    /**
     * @return the user owner of the entity to be compared with current user for access check
     */
    public ProvolyUser getOwner();
}
