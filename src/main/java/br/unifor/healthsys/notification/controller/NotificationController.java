package br.unifor.healthsys.notification.controller;

import br.unifor.healthsys.notification.messaging.NotificationEventProducer;
import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificações", description = "Timeline de notificações e broadcast de eventos clínicos")
public class NotificationController {

    private final NotificationEventProducer eventProducer;
    private final NotificationTimelineService notificationTimelineService;

    public NotificationController(NotificationEventProducer eventProducer,
                                  NotificationTimelineService notificationTimelineService) {
        this.eventProducer = eventProducer;
        this.notificationTimelineService = notificationTimelineService;
    }

    @GetMapping
    @Operation(summary = "Lista notificações por perfil", description = "Retorna a timeline paginada para o perfil do usuário. Paginação via cabeçalhos X-Total-Count/X-Page/X-Page-Size.")
    @PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO')")
    public ResponseEntity<List<Notification>> findAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            Authentication authentication) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        String role = currentRole(authentication);
        Page<Notification> result = notificationTimelineService.findForRole(role, safePage, safeSize);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Page", String.valueOf(result.getNumber()))
                .header("X-Page-Size", String.valueOf(result.getSize()))
                .body(result.getContent());
    }

    private String currentRole(Authentication authentication) {
        if (authentication == null) return null;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring(5);
            }
        }
        return null;
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Publica uma notificação", description = "Envia uma notificação para o tópico de eventos, distribuída em tempo real via WebSocket.")
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

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        return "pong: " + message;
    }
}
