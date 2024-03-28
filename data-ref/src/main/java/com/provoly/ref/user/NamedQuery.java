package com.provoly.ref.user;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityShared;
import com.provoly.ref.searchrequest.SearchRequest;

@Entity
public class NamedQuery extends EntityShared {
    private String description;

    @OneToMany(mappedBy = "namedQuery", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "user")
    private Map<ProvolyUser, ProvolyUserNamedQuery> belongTo = new HashMap<>();

    @ManyToOne(cascade = CascadeType.ALL)
    private SearchRequest request;

    protected NamedQuery() {
        super();
    }

    @Default
    public NamedQuery(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public void add(ProvolyUser user, boolean isOwner) {
        belongTo.putIfAbsent(user, new ProvolyUserNamedQuery(user, this, isOwner));
    }

    public void remove(ProvolyUser user) {
        belongTo.remove(user);
    }

    /**
     * Always add current user if level is public, then try to get his
     * named query. If level is private and the named query not belong to user,
     * raise exception.
     */
    public ProvolyUserNamedQuery getForUser(ProvolyUser user) {
        if (isPublic()) {
            add(user, false);
        }
        var userNamedQuery = belongTo.get(user);
        if (userNamedQuery == null) {
            throw new BusinessException(ErrorCode.TECHNICAL, "NamedQuery %s/%s doesn't belong to user %s"
                    .formatted(this.id, this.name, user.getId()));
        }
        return userNamedQuery;
    }

    public boolean belongToNobody() {
        return belongTo.isEmpty();
    }

    public boolean isOwner(ProvolyUser user) {
        if (belongTo.get(user) != null) {
            return belongTo.get(user).isOwner();
        }
        return false;
    }

    public void setFavoriteFor(ProvolyUser user, boolean favorite) {
        if (isPublic() && belongTo.get(user) == null) {
            if (!favorite) {
                throw new BusinessException(ErrorCode.TECHNICAL,
                        "NamedQuery not in user %s's favorite list".formatted(user.getId()));
            }
            add(user, false);
        }
        getForUser(user).setFavorite(favorite);
    }

    public void setColorFor(ProvolyUser user, String color) {
        if (isPublic() && belongTo.get(user) == null) {
            add(user, false);
        }
        getForUser(user).setColor(color);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SearchRequest getRequest() {
        return request;
    }

    public void setRequest(SearchRequest searchRequest) {
        this.request = searchRequest;
    }

    @Override
    public String toString() {
        return "id = %s ; name = %s ; description = %s ;".formatted(id, name, description);
    }
}
