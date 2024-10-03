package com.provoly.ref.message.notification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.message.notification.model.Notification;
import com.provoly.ref.message.notification.model.ProvolyUserNotification;
import com.provoly.ref.message.notification.model.ProvolyUserNotification_;
import com.provoly.ref.user.ProvolyUser_;

@ApplicationScoped
public class NotificationRepository {
    private EntityIdRepository entityIdRepository;

    @PersistenceContext
    private EntityManager em;

    public NotificationRepository(EntityIdRepository entityIdRepository, EntityManager em) {
        this.entityIdRepository = entityIdRepository;
        this.em = em;
    }

    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(entityIdRepository.findById(id, Notification.class));
    }

    public Notification getById(UUID id) {
        return entityIdRepository.getById(id, Notification.class);
    }

    public List<Notification> getAllNotifications() {
        return entityIdRepository.getAll(Notification.class);
    }

    public void save(Notification notification) {
        em.persist(notification);
    }

    public void removeNotification(Notification notification) {
        entityIdRepository.removeEntity(notification);
    }

    public List<ProvolyUserNotification> getNotificationsForUsers(Set<UUID> usersId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<ProvolyUserNotification> query = cb.createQuery(ProvolyUserNotification.class);
        Root<ProvolyUserNotification> queryRoot = query.from(ProvolyUserNotification.class);
        query.where(queryRoot.get(ProvolyUserNotification_.USER).get(ProvolyUser_.ID).in(usersId));

        return em.createQuery(query).getResultList();
    }
}
