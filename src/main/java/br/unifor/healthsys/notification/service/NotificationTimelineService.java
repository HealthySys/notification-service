package br.unifor.healthsys.notification.service;

import br.unifor.healthsys.notification.model.Notification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotificationTimelineService {

    private static final int MAX_NOTIFICATIONS = 100;

    private final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();

    public Notification register(Notification notification) {
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }

        if (notification.getTimestamp() == null) {
            notification.setTimestamp(LocalDateTime.now());
        }

        notifications.addFirst(notification);

        while (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.removeLast();
        }

        return notification;
    }

    public List<Notification> findAll() {
        return new ArrayList<>(notifications);
    }

    public void clear() {
        notifications.clear();
    }
}
