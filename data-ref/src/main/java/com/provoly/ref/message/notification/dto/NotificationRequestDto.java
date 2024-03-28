package com.provoly.ref.message.notification.dto;

import java.util.List;
import java.util.UUID;

public record NotificationRequestDto(UUID id, List<UUID> users, NotificationTextDto notificationTextDto, String link) {

}