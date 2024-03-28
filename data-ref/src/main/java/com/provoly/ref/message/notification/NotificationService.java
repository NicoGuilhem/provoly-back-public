package com.provoly.ref.message.notification;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.dto.DashboardDto;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.message.Message;
import com.provoly.ref.message.MessageListener;
import com.provoly.ref.message.MessageService;
import com.provoly.ref.message.notification.dto.NotificationRequestDto;
import com.provoly.ref.message.notification.dto.NotificationTextDto;
import com.provoly.ref.message.notification.model.*;
import com.provoly.ref.message.websocket.MessageSocketServer;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.ProvolyUser_;
import com.provoly.ref.user.UserService;

import io.quarkus.security.identity.SecurityIdentity;

import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService implements MessageListener {

    private static Map<UUID, Set<UUID>> userByNotificationMap = new HashMap<>();
    private SecurityIdentity identity;

    private UserService userService;

    private NotificationMapper notificationMapper;

    private MessageService messageService;

    private MessageSocketServer messageSocketServer;
    private Logger log;

    private EntityIdService entityIdService;
    @PersistenceContext
    EntityManager em;

    public NotificationService(SecurityIdentity securityIdentity, UserService userService,
            NotificationMapper notificationMapper, MessageService messageService, MessageSocketServer messageSocketServer,
            Logger log, EntityIdService entityIdService) {
        this.identity = securityIdentity;
        this.userService = userService;
        this.notificationMapper = notificationMapper;
        this.messageService = messageService;
        this.messageSocketServer = messageSocketServer;
        this.log = log;
        this.entityIdService = entityIdService;
    }

    public Notification findById(UUID id) {
        return entityIdService.findById(id, Notification.class);
    }

    public Notification getById(UUID id) {
        return entityIdService.getById(id, Notification.class);
    }

    public List<Notification> getAllNotifications() {
        return entityIdService.getAll(Notification.class);
    }

    @Transactional
    public void addNotification(NotificationRequestDto notificationRequestDto) {
        if (findById(notificationRequestDto.id()) == null) {
            var users = entityIdService.getAll(ProvolyUser.class).stream()
                    .collect(Collectors.toMap(ProvolyUser::getId, Function.identity(), (a, b) -> a));
            var notification = notificationMapper.toModel(notificationRequestDto);
            notificationRequestDto.users().stream()
                    .filter(users::containsKey)
                    .forEach(userId -> notification.addUser(users.get(userId)));
            em.persist(notification);
        } else {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE,
                    MessageFormat.format("Notification id=[{0}] not modifiable", notificationRequestDto.id()));
        }
    }

    @Transactional
    public void acknowledgeNotification(UUID id) {
        var currentUser = userService.getCurrentUser();
        var notification = getById(id);
        if (notification.getForUser(currentUser) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    MessageFormat.format("Notification {0} has not linked with user {1}", notification.getId(),
                            currentUser.getId()));
        }
        notification.remove(currentUser);
        deleteFromKnownNotifications(id, currentUser.getId());

        if (notification.belongToNobody()) {
            entityIdService.removeEntity(notification);
        }
    }

    @Override
    public void onMessage(Message message) {
        // nothing to do
    }

    @Override
    public void onConnection(String sessionId) {
        if (identity.hasRole(Role.STR_CLASS_READ_NOTIFICATION)) {
            sendAllMessagesForMe(sessionId);
        } else {
            log.infof("Current user name=[%s] is not authorized", identity.getPrincipal().getName());
        }
    }

    @Transactional
    public void acknowledgeAllNotificationForCurrentUser() {
        getAllNotificationsForCurrentUser().stream().forEach(notif -> acknowledgeNotification(notif.getId()));
    }

    private void sendAllMessagesForMe(String sessionId) {
        var notifications = getAllNotificationsForCurrentUser();

        var currentUser = userService.getCurrentUser();
        for (Notification notification : notifications) {
            addToKnownNotifications(notification.getId(), new HashSet<>(List.of(currentUser.getId())));
            messageService.sendMessage(messageService.createMessage(notification), sessionId);
        }
    }

    private List<Notification> getAllNotificationsForCurrentUser() {
        var currentUser = userService.getCurrentUser();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Notification> query = builder.createQuery(Notification.class);
        Root<Notification> notificationRoot = query.from(Notification.class);
        Join<Notification, ProvolyUserNotification> joinProvolyUserNotification = notificationRoot.join(Notification_.belongTo);
        query.select(notificationRoot)
                .where(builder.equal(joinProvolyUserNotification.get(ProvolyUserNotification_.USER), currentUser))
                .orderBy(builder.desc(notificationRoot.get(Notification_.creationDate)));

        return em.createQuery(query).getResultList();
    }

    @Transactional
    public void sendNotification(DashboardDto dashboard, boolean isCreated) {
        NotificationMessageCode messageCode;
        if (isCreated) {
            messageCode = dashboard.getGroups() == null || dashboard.getGroups().isEmpty()
                    ? NotificationMessageCode.DASHBOARD_PRIVATE
                    : NotificationMessageCode.DASHBOARD_PUBLIC;
        } else {
            // dashboard is deleted
            messageCode = NotificationMessageCode.DASHBOARD_DELETED;
        }

        var userIds = userService.getAllUserIdExceptCurrent();

        // create and send a notification
        var parameterMap = new HashMap<String, String>();
        parameterMap.put("name", dashboard.getName());
        saveNotification(messageCode, null, userIds, parameterMap);
    }

    private void saveNotification(NotificationMessageCode messageCode, String link, List<UUID> users, Map parameterMap) {
        var notificationTextDto = new NotificationTextDto(messageCode, parameterMap);
        var notification = new NotificationRequestDto(UUID.randomUUID(), users, notificationTextDto, link);
        addNotification(notification);
    }

    private List<ProvolyUserNotification> getUsersNotification(Set<UUID> usersId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<ProvolyUserNotification> query = cb.createQuery(ProvolyUserNotification.class);
        Root<ProvolyUserNotification> queryRoot = query.from(ProvolyUserNotification.class);
        query.where(queryRoot.get(ProvolyUserNotification_.USER).get(ProvolyUser_.ID).in(usersId));

        return em.createQuery(query).getResultList();
    }

    private void deleteFromKnownNotifications(UUID notificationId, UUID userId) {
        userByNotificationMap.get(notificationId).remove(userId);
        if (userByNotificationMap.get(notificationId).isEmpty())
            userByNotificationMap.remove(notificationId);
    }

    public boolean isNotificationKnown(UUID notificationId) {
        return userByNotificationMap.containsKey(notificationId);
    }

    private void addToKnownNotifications(UUID notificationId, Set<UUID> userId) {
        userByNotificationMap.merge(notificationId, userId, (old, value) -> {
            Set<UUID> newSet = new HashSet<>();
            newSet.addAll(old);
            newSet.addAll(value);
            return newSet;
        });
    }

    public void refreshNotifications() {
        // Get all connected users
        Map<UUID, List<Session>> sessions = messageSocketServer.getSessionsById();

        // Get all notifications associated to users
        List<ProvolyUserNotification> notificationList = getUsersNotification(sessions.keySet());

        // Filter and set a new Map of new Notification with users
        Map<Notification, Set<UUID>> newNotificationsMap = getUnknownNotification(notificationList);

        // Send new notifications and save in replica map
        newNotificationsMap.forEach((key, value) -> {
            messageService.sendMessage(messageService.createMessage(key), value.stream().toList());
            addToKnownNotifications(key.getId(), value);
        });
    }

    private Map<Notification, Set<UUID>> getUnknownNotification(List<ProvolyUserNotification> notificationList) {
        Map<Notification, Set<UUID>> notificationFromDatabase = notificationList.stream()
                .collect(Collectors.groupingBy(
                        ProvolyUserNotification::getNotification,
                        Collectors.mapping(
                                provolyUserNotification -> provolyUserNotification.getUser().getId(),
                                Collectors.toSet())));

        Map<Notification, Set<UUID>> result = new HashMap<>();
        notificationFromDatabase.forEach((notification, userIds) -> {
            if (userByNotificationMap.get(notification.getId()) != null) {
                userIds.removeAll(userByNotificationMap.get(notification.getId()));
            }
            result.put(notification, userIds);
        });

        return result;
    }
}