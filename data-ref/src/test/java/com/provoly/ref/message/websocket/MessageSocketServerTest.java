package com.provoly.ref.message.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.user.Role;
import com.provoly.ref.message.notification.NotificationController;
import com.provoly.ref.message.notification.NotificationService;
import com.provoly.ref.message.notification.dto.NotificationRequestDto;
import com.provoly.ref.message.notification.dto.NotificationTextDto;
import com.provoly.ref.message.notification.model.NotificationMessageCode;

import io.quarkus.security.ForbiddenException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class MessageSocketServerTest {
    @InjectMock
    MessageSocketServer messageSocketServer;

    @Inject
    NotificationService notificationService;

    @Inject
    NotificationController notificationController;

    @BeforeEach
    public void setup() {
        messageSocketServer = Mockito.mock(MessageSocketServer.class);
        QuarkusMock.installMockForType(messageSocketServer, MessageSocketServer.class);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE_NOTIFICATION })
    public void testWebsocketAuthenticationSuccess() {
        int notifSize = notificationService.getAllNotifications().size();
        notificationController.addNotification(createNotification());

        assertThat(notificationService.getAllNotifications().size()).isEqualTo(notifSize + 1);
    }

    @Test
    @TestSecurity(user = "testUserNotAuthorized", roles = { Role.STR_CLASS_READ_NOTIFICATION })
    public void testWebsocketAuthenticationFailed() {
        assertThatThrownBy(() -> {
            notificationController.addNotification(createNotification());
        }).isInstanceOf(ForbiddenException.class);
    }

    private NotificationRequestDto createNotification() {
        var paramMap = new HashMap<String, String>();
        paramMap.put("key", "value");
        var notificationTextDto = new NotificationTextDto(NotificationMessageCode.DASHBOARD_PRIVATE.name(),
                paramMap);
        return new NotificationRequestDto(UUID.randomUUID(), List.of(), notificationTextDto,
                "/link");
    }
}
