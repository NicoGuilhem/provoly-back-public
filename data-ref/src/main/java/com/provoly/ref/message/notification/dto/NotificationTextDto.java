package com.provoly.ref.message.notification.dto;

import java.util.Map;

import com.provoly.ref.message.notification.model.NotificationMessageCode;

public record NotificationTextDto(NotificationMessageCode code, Map<String, String> param) {

}