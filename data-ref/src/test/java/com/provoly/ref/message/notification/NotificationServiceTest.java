package com.provoly.ref.message.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.user.Role;
import com.provoly.ref.message.MessageService;
import com.provoly.ref.message.notification.dto.NotificationRequestDto;
import com.provoly.ref.message.notification.dto.NotificationTextDto;
import com.provoly.ref.message.notification.model.NotificationMessageCode;
import com.provoly.ref.user.UserService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @Inject
    UserService userService;

    @Inject
    TestService testService;

    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;

    @InjectMock
    MessageService messageService;

    @BeforeEach
    public void authenticate() {
        testService.authenticate(currentSubjectProvider);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE_NOTIFICATION, Role.STR_CLASS_READ_NOTIFICATION })
    public void should_sendMessageOnlyOnceToUserAndRegisterNotification() {
        var notificationDto = createNotification();
        var notification = notificationService.getById(notificationDto.id());

        notificationService.refreshNotifications();

        Mockito.verify(messageService).createMessage(notification);
        assertThat(notificationService.isNotificationKnown(notificationDto.id())).isTrue();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE_NOTIFICATION, Role.STR_CLASS_READ_NOTIFICATION })
    public void should_deleteNotification() {
        var notification = createNotification();

        notificationService.refreshNotifications();
        notificationService.acknowledgeNotification(notification.id());

        assertThrows(BusinessException.class, () -> notificationService.getById(notification.id()));
    }

    private NotificationRequestDto createNotification() {
        NotificationTextDto textDto = new NotificationTextDto(NotificationMessageCode.DASHBOARD_PRIVATE.name(),
                new HashMap<>());
        var users = List.of(userService.getCurrentUser().getId());
        var dto = new NotificationRequestDto(UUID.randomUUID(), users, textDto, "link");
        notificationService.saveNotification(dto);
        return dto;
    }

}