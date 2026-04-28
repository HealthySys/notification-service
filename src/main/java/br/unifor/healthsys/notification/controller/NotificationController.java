package br.unifor.healthsys.notification.controller;

import br.unifor.healthsys.notification.messaging.NotificationEventProducer;
import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationEventProducer eventProducer;
    private final NotificationTimelineService notificationTimelineService;

    public NotificationController(NotificationEventProducer eventProducer,
                                  NotificationTimelineService notificationTimelineService) {
        this.eventProducer = eventProducer;
        this.notificationTimelineService = notificationTimelineService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO','ADMIN')")
    public ResponseEntity<List<Notification>> findAll() {
        return ResponseEntity.ok(notificationTimelineService.findAll());
    }

    /**
     * Endpoint REST para envio manual de notificações (admin/teste)
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO','ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody Notification notification) {
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }

        eventProducer.publish(notification);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "accepted", true,
                "messageId", notification.getId(),
                "topic", NotificationEventProducer.NOTIFICATIONS_TOPIC
        ));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clear() {
        notificationTimelineService.clear();
        return ResponseEntity.noContent().build();
    }

    /**
     * Handler WebSocket - cliente envia mensagem para /app/ping, recebe em /topic/pong
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        return "pong: " + message;
    }
}
