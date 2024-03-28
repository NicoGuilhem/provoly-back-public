package com.provoly.ref.schedulers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.ref.message.notification.NotificationService;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class NotificationScheduler {

    @Inject
    NotificationService notificationService;

    @Scheduled(every = "{provoly.ref.scheduler.notification}")
    public void refreshNotifications() {
        notificationService.refreshNotifications();
    }
}
