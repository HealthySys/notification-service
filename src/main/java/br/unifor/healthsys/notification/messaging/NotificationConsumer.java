package br.unifor.healthsys.notification.messaging;

import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationEventProducer eventProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationTimelineService notificationTimelineService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationEventProducer eventProducer,
                                SimpMessagingTemplate messagingTemplate,
                                NotificationTimelineService notificationTimelineService,
                                ObjectMapper objectMapper) {
        this.eventProducer = eventProducer;
        this.messagingTemplate = messagingTemplate;
        this.notificationTimelineService = notificationTimelineService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "triagem-events", groupId = "notification-triage-group")
    public void consumeTriageEvent(Map<String, Object> event) {
        String correlationId = String.valueOf(event.getOrDefault("correlationId", ""));
        if (!notificationTimelineService.markTriageEventQueued(correlationId)) {
            log.warn("Evento Kafka duplicado descartado. correlationId={}", correlationId);
            return;
        }

        String classification = String.valueOf(event.getOrDefault("riskClassification", ""));
        String patientName = String.valueOf(event.getOrDefault("patientName", "Paciente"));

        Notification notification = Notification.builder()
                .type("TRIAGE_COMPLETED")
                .title("Triagem concluída")
                .message(String.format("Triagem de %s concluída. Classificação: %s", patientName, classification))
                .severity(isCritical(classification) ? "CRITICAL" : "INFO")
                .id(correlationId.isBlank() ? null : correlationId)
                .correlationId(correlationId)
                .patientName(patientName)
                .patientId(event.get("patientId") instanceof Number number ? number.longValue() : null)
                .build();

        eventProducer.publish(notification);
        log.info("Evento de triagem encaminhado ao Kafka para notificacao. correlationId={}", correlationId);
    }

    @KafkaListener(topics = NotificationEventProducer.NOTIFICATIONS_TOPIC, groupId = "notification-broadcast-group")
    public void consumeNotification(Map<String, Object> payload) {
        Notification notification = objectMapper.convertValue(payload, Notification.class);

        if (!notificationTimelineService.markProcessed(notification.getId())) {
            log.warn("Notificacao duplicada descartada. id={}", notification.getId());
            return;
        }

        notificationTimelineService.register(notification);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        if ("CRITICAL".equals(notification.getSeverity())) {
            messagingTemplate.convertAndSend("/topic/alerts", notification);
            log.warn("Alerta critico broadcast: {}", notification.getMessage());
        }
    }

    private boolean isCritical(String classification) {
        return "VERMELHO".equals(classification) || "LARANJA".equals(classification);
    }
}
