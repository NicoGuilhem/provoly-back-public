package com.provoly.ref.message.websocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.message.Message;
import com.provoly.ref.message.MessageListener;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/messages")
@ApplicationScoped
public class MessageSocketServer {

    private static final String USERID = "userid";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Logger log;

    Map<UUID, List<Session>> sessionsById = new ConcurrentHashMap<>();

    @Inject
    Instance<MessageListener> messageListeners;

    @Inject
    UserService userService;

    @OnOpen
    public void onOpen(Session session) {
        ProvolyUser currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return; // user not authenticate
        }

        var currentUserId = currentUser.getId();
        session.getUserProperties().put(USERID, currentUserId);

        var userSessions = sessionsById.get(currentUserId);
        if (userSessions == null) {
            var sessions = new ArrayList<Session>();
            sessions.add(session);
            sessionsById.put(currentUserId, sessions);
        } else {
            userSessions.add(session);
        }

        log.infof("User id=[%s]/session id=[%s] name=[%s] connexion",
                currentUserId,
                session.getId(),
                session.getUserPrincipal().getName());

        messageListeners.forEach(m -> m.onConnection(session.getId()));
    }

    @OnClose
    public void onClose(Session session) {
        removeSession(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.warn("Error on websocket", throwable);
        removeSession(session);
    }

    private void removeSession(Session session) {
        var userId = getCurrentUserId(session);
        var userSessions = sessionsById.getOrDefault(userId, List.of());

        var sessionRemoved = false;
        var it = userSessions.iterator();
        while (it.hasNext()) {
            var sess = it.next();
            if (sess.getId().equals(session.getId())) {
                it.remove();
                sessionRemoved = true;
            }
        }
        if (!sessionRemoved) {
            throw new BusinessException(ErrorCode.TECHNICAL, "An session is missing Principal : " + session.getUserPrincipal());
        }

        log.infof("User id=[%s]/session id=[%s] name=[%s] left",
                userId,
                session.getId(),
                session.getUserPrincipal().getName());
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        log.debugf("Message from user id=[%s] name=[%s] with message %s",
                session.getUserProperties().get(USERID),
                session.getUserPrincipal().getName(),
                msg);
        messageListeners.forEach(m -> {
            m.onMessage(null);
        });
    }

    /**
     * Send messages to all users.
     *
     * @param message list of message
     * @param users the list of users of message destination
     */
    public void sendMessage(Message message, List<UUID> users) {
        users.forEach(u -> sendMessage(message, u));
    }

    private void sendMessage(Message message, UUID userId) {
        sessionsById.getOrDefault(userId, List.of()).forEach(s -> sendMessage(message, s));
    }

    public void sendMessage(Message message, String sessionId) {
        sessionsById.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(session -> session.getId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> sendMessage(message, session));
    }

    private void sendMessage(Message message, Session session) {
        try {
            var msg = objectMapper.writeValueAsString(message);
            session.getAsyncRemote().sendObject(msg, result -> {
                if (result.isOK()) {
                    log.debugf("Message %s has sent to user id=[%s] name=[%s]",
                            msg,
                            getCurrentUserId(session),
                            session.getUserPrincipal().getName());
                } else {
                    log.warnf("Unable to send message: %s", result.getException());
                }
            });
        } catch (JsonProcessingException e) {
            log.errorf("An error while serializing message to JSON");
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to serialize message " + message, e);
        }

    }

    /**
     * Get the current user identifier stored into session property.
     *
     * @param session the Websocket session
     * @return the current user identifier stored into session property
     */
    private UUID getCurrentUserId(Session session) {
        return (UUID) session.getUserProperties().get(USERID);
    }

    public Map<UUID, List<Session>> getSessionsById() {
        return sessionsById;
    }

}