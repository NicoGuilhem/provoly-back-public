package com.provoly.ref.message.notification;

import java.time.Instant;

import com.provoly.ref.message.notification.dto.NotificationTextDto;

/**
 *
 * @param text contains the entire text of the notification including title and description
 * @param link notification redirection link
 * @param creationDate
 * @param creator user who initiated the notification
 */
public record ExternalNotificationPayload(NotificationTextDto text, String link, Instant creationDate, String creator) {
}