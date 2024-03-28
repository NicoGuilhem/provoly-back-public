package com.provoly.ref.message.notification;

import java.time.Instant;

import com.provoly.ref.message.notification.dto.NotificationTextDto;

public record NotificationPayload(NotificationTextDto text, String link, Instant creationDate) {
}