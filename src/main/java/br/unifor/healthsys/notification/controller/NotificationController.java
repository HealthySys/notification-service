package br.unifor.healthsys.notification.controller;

import br.unifor.healthsys.notification.model.Notification;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Endpoint REST para envio manual de notificacoes (admin/teste)
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody Notification notification) {
        notification.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/notifications", notification);
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
