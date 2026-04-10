package br.unifor.healthsys.notification.controller;

import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationTimelineService notificationTimelineService;

    public NotificationController(SimpMessagingTemplate messagingTemplate,
                                  NotificationTimelineService notificationTimelineService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationTimelineService = notificationTimelineService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> findAll() {
        return ResponseEntity.ok(notificationTimelineService.findAll());
    }

    /**
     * Endpoint REST para envio manual de notificações (admin/teste)
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Notification> broadcast(@RequestBody Notification notification) {
        notification = notificationTimelineService.register(notification);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @DeleteMapping
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
