package com.provoly.ref.message.notification.dto;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @param title notification title. Corresponds to titleCode from Notification Entit
 * @param code notification description. Correspond to messageCode from Notification Entity
 * @param param if title or code contain a translation code, param is used to enter values in the translation message
 */
public record NotificationTextDto(String title,
        String code,
        Map<String, String> param) {

    public NotificationTextDto {
        param = new HashMap<>();
    }

    public NotificationTextDto(String code, Map<String, String> param) {
        this(null, code, param);
    }
}