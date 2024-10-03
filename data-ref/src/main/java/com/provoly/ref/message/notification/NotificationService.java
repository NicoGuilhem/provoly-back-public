package com.provoly.ref.message.notification;

import java.text.MessageFormat;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.dto.DashboardDto;
import com.provoly.ref.message.Message;
import com.provoly.ref.message.MessageListener;
import com.provoly.ref.message.MessageService;
import com.provoly.ref.message.notification.dto.NotificationRequestDto;
import com.provoly.ref.message.notification.dto.NotificationTextDto;
import com.provoly.ref.message.notification.model.Notification;
import com.provoly.ref.message.notification.model.NotificationMessageCode;
import com.provoly.ref.message.notification.model.ProvolyUserNotification;
import com.provoly.ref.message.websocket.MessageSocketServer;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService implements MessageListener {

    private static Map<UUID, Set<UUID>> usersByKnownNotifications = new HashMap<>(); // Map<NotificationId,Set<userIds>>, to avoid sending the same notifications to users

    private SecurityIdentity identity;
    private NotificationRepository notificationRepository;
    private UserService userService;
    private NotificationMapper notificationMapper;
    private MessageService messageService;
    private MessageSocketServer messageSocketServer;
    private Logger log;

    public NotificationService(SecurityIdentity securityIdentity,
            UserService userService,
            NotificationMapper notificationMapper,
            MessageService messageService,
            MessageSocketServer messageSocketServer,
            Logger log,
            NotificationRepository notificationRepository) {
        this.identity = securityIdentity;
        this.userService = userService;
        this.notificationMapper = notificationMapper;
        this.messageService = messageService;
        this.messageSocketServer = messageSocketServer;
        this.log = log;
        this.notificationRepository = notificationRepository;
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

    @Incoming("notifications")
    public void receiveNotifications(ExternalNotificationPayload payload) {
        log.debugf("Receive new notification from topic, save it");
        var userIds = userService.getAllUserIdsExcept(payload.creator());
        var notification = new NotificationRequestDto(UUID.randomUUID(), userIds, payload.text(), payload.link());
        saveNotification(notification);
    }

    public void sendNotification(DashboardDto dashboard, boolean isCreated) {
        log.infof("Save notification for dashboard %s to all users except current", dashboard.getName());
        NotificationMessageCode messageCode = isCreated
                ? getPublicOrPrivateCode(dashboard)
                : NotificationMessageCode.DASHBOARD_DELETED;
        var userIds = userService.getAllUserIdsExcept(userService.getCurrentUser().getSubject());

        var notificationTextDto = new NotificationTextDto(messageCode.name(), Map.of("name", dashboard.getName()));
        var notification = new NotificationRequestDto(UUID.randomUUID(), userIds, notificationTextDto, null);
        saveNotification(notification);
    }

    private NotificationMessageCode getPublicOrPrivateCode(DashboardDto dashboard) {
        return dashboard.getAccessRightsByGroup() == null || dashboard.getAccessRightsByGroup().isEmpty()
                ? NotificationMessageCode.DASHBOARD_PRIVATE
                : NotificationMessageCode.DASHBOARD_PUBLIC;
    }

    @Transactional
    public void saveNotification(NotificationRequestDto notificationRequestDto) {
        if (notificationRepository.findById(notificationRequestDto.id()).isPresent()) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE,
                    MessageFormat.format("Notification id=[{0}] not modifiable", notificationRequestDto.id()));
        }
        var notification = notificationMapper.toModel(notificationRequestDto);
        userService.getAllUsersWithIds(notificationRequestDto.users())
                .forEach(notification::addUser);
        notificationRepository.save(notification);
    }

    @Transactional
    public void acknowledgeNotification(UUID id) {
        log.infof("Acknowledge notification %s for current user", id);
        var currentUser = userService.getCurrentUser();
        var notification = getById(id);
        if (notification.getForUser(currentUser) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    MessageFormat.format("Notification {0} has not linked with user {1}", notification.getId(),
                            currentUser.getId()));
        }
        deleteNotification(notification, currentUser);
    }

    @Transactional
    public void acknowledgeAllNotificationsForCurrentUser() {
        log.info("Acknowledge all notifications for current user");
        var currentUser = userService.getCurrentUser();
        notificationRepository.getNotificationsForUsers(Set.of(currentUser.getId()))
                .forEach(association -> deleteNotification(association.getNotification(), currentUser));
    }

    @Transactional
    public Notification getById(UUID id) {
        return notificationRepository.getById(id);
    }

    @Transactional
    public List<Notification> getAllNotifications() {
        return notificationRepository.getAllNotifications();
    }

    @Transactional
    public void refreshNotifications() {
        log.debug("Refresh notifications for all connected users");

        // Get all connected users
        var connectedUserIds = messageSocketServer.getSessionsById().keySet();

        log.debug("Get all notifications associated to users");
        List<ProvolyUserNotification> userNotificationsAssociation = notificationRepository
                .getNotificationsForUsers(connectedUserIds);

        log.debug("Send unknown notifications only for connected user");
        userNotificationsAssociation.forEach(association -> {
            UUID notificationId = association.getNotification().getId();
            if (usersByKnownNotifications.containsKey(notificationId)) {
                log.infof("Notification %s already sent to some users, send it only to new connected user", notificationId);
                connectedUserIds.removeAll(usersByKnownNotifications.get(notificationId));
            }
            messageSocketServer.sendMessage(messageService.createMessage(association.getNotification()), connectedUserIds);
            addToKnownNotifications(notificationId, connectedUserIds);
        });
    }

    private void sendAllMessagesForMe(String sessionId) {
        log.debugf("Get all notifications for user", sessionId);
        var currentUser = userService.getCurrentUser();
        var associations = notificationRepository.getNotificationsForUsers(Set.of(currentUser.getId()));
        associations.forEach(association -> {
            addToKnownNotifications(association.getNotification().getId(), List.of(currentUser.getId()));
            messageService.sendMessage(messageService.createMessage(association.getNotification()), sessionId);
        });
    }

    private void deleteNotification(Notification notification, ProvolyUser currentUser) {
        notification.remove(currentUser);
        deleteFromKnownNotifications(notification.getId(), currentUser.getId());

        if (notification.belongToNobody()) {
            log.debugf("Notification %s is acknowledged for all users, delete it", notification.getId());
            notificationRepository.removeNotification(notification);
        }
    }

    private void deleteFromKnownNotifications(UUID notificationId, UUID userId) {
        usersByKnownNotifications.get(notificationId).remove(userId);
        if (usersByKnownNotifications.get(notificationId).isEmpty()) {
            usersByKnownNotifications.remove(notificationId);
        }
    }

    public boolean isNotificationKnown(UUID notificationId) {
        return usersByKnownNotifications.containsKey(notificationId);
    }

    private void addToKnownNotifications(UUID notificationId, Collection<UUID> userIds) {
        usersByKnownNotifications.computeIfAbsent(notificationId, user -> new HashSet<>()).addAll(userIds);
    }
}