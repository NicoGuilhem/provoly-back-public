package com.provoly.ref.message.notification.model;

import java.time.Instant;
import java.util.*;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.user.ProvolyUser;

import org.hibernate.annotations.CreationTimestamp;

@Entity
public class Notification extends EntityId {

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "user")
    private Map<ProvolyUser, ProvolyUserNotification> belongTo = new HashMap<>();

    private String link;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant creationDate;

    private String titleCode; // either a title translation code or directly the text if not internationalized

    private String messageCode; // either a message translation code or directly the text if not internationalized

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<NotificationParameter> parameterValues = new ArrayList<>();

    protected Notification() {
        super();
    }

    @Default
    public Notification(UUID id) {
        super(id);
    }

    public void remove(ProvolyUser user) {
        belongTo.remove(user);
    }

    public ProvolyUserNotification getForUser(ProvolyUser user) {
        var userNotification = belongTo.get(user);
        if (userNotification == null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Notification " + this.id + "/ with message code" + this.messageCode + " not belong to user "
                            + user.getId());
        }
        return userNotification;
    }

    public void addUser(ProvolyUser user) {
        belongTo.putIfAbsent(user, new ProvolyUserNotification(user, this));
    }

    public boolean belongToNobody() {
        return belongTo.isEmpty();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Map<ProvolyUser, ProvolyUserNotification> getBelongTo() {
        return belongTo;
    }

    public void setBelongTo(Map<ProvolyUser, ProvolyUserNotification> belongTo) {
        this.belongTo = belongTo;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public String getTitleCode() {
        return titleCode;
    }

    public void setTitleCode(String titleCode) {
        this.titleCode = titleCode;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public List<NotificationParameter> getParameterValues() {
        return parameterValues;
    }

    public void setParameterValues(List<NotificationParameter> parameterValues) {
        this.parameterValues = parameterValues;
    }
}