package br.unifor.healthsys.notification.messaging;

import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationTimelineService notificationTimelineService;

    public NotificationConsumer(SimpMessagingTemplate messagingTemplate,
                                NotificationTimelineService notificationTimelineService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationTimelineService = notificationTimelineService;
    }

    @KafkaListener(topics = "healthsys.notifications", groupId = "notification-service-group")
    public void consumeNotification(Map<String, Object> event) {
        log.info("Notificacao recebida: tipo={}", event.get("type"));

        Notification notification = Notification.fromEvent(event);
        notificationTimelineService.register(notification);

        // Broadcast para todos os clientes conectados
        messagingTemplate.convertAndSend("/topic/notifications", notification);

        // Canal especifico para alertas criticos
        if ("CRITICAL".equals(notification.getSeverity())) {
            messagingTemplate.convertAndSend("/topic/alerts/critical", notification);
            log.warn("Alerta critico broadcast: {}", notification.getMessage());
        }
    }

    @KafkaListener(topics = "healthsys.triage.events", groupId = "notification-triage-group")
    public void consumeTriageEvent(Map<String, Object> event) {
        String classification = String.valueOf(event.getOrDefault("riskClassification", ""));
        String patientName = String.valueOf(event.getOrDefault("patientName", "Paciente"));

        Notification notification = Notification.builder()
                .type("TRIAGE_COMPLETED")
                .title("Triagem concluída")
                .message(String.format("Triagem de %s concluída. Classificação: %s", patientName, classification))
                .severity(isCritical(classification) ? "WARNING" : "INFO")
                .patientName(patientName)
                .build();

        notificationTimelineService.register(notification);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.info("Notificacao de triagem enviada via WebSocket para: {}", patientName);
    }

    private boolean isCritical(String classification) {
        return "VERMELHO".equals(classification) || "LARANJA".equals(classification);
    }
}
