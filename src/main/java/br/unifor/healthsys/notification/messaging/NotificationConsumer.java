package br.unifor.healthsys.notification.messaging;

import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.service.NotificationTimelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
        String type = String.valueOf(event.getOrDefault("type", "TRIAGEM_CLASSIFICADA"));
        String eventId = String.valueOf(event.getOrDefault("eventId", ""));
        String correlationId = String.valueOf(event.getOrDefault("correlationId", ""));
        String patientName = String.valueOf(event.getOrDefault("patientName", "Paciente"));
        Long patientId = event.get("patientId") instanceof Number n ? n.longValue() : null;
        Long triageId = event.get("triageId") instanceof Number n ? n.longValue() : null;

        if ("ATENDIMENTO_INICIADO".equals(type)) {
            String medicoName = String.valueOf(event.getOrDefault("medicoName", "médico"));
            Notification notification = Notification.builder()
                    .id(eventId.isBlank() ? UUID.randomUUID().toString() : eventId)
                    .type("ATTENDANCE_STARTED")
                    .title("Paciente em atendimento")
                    .message(String.format("%s já está sendo atendido por %s.", patientName, medicoName))
                    .severity("INFO")
                    .correlationId(correlationId)
                    .patientName(patientName)
                    .patientId(patientId)
                    .triageId(triageId)
                    .targetRoles(List.of("MEDICO"))
                    .build();
            eventProducer.publish(notification);
            log.info("Evento ATENDIMENTO_INICIADO encaminhado. correlationId={} triageId={}", correlationId, triageId);
            return;
        }

        if ("PACIENTE_ENCAMINHADO".equals(type)) {
            String forwardedByName = String.valueOf(event.getOrDefault("forwardedByName", "recepção"));
            Notification notification = Notification.builder()
                    .id(eventId.isBlank() ? UUID.randomUUID().toString() : eventId)
                    .type("PATIENT_FORWARDED")
                    .title("Paciente aguardando triagem")
                    .message(String.format("%s foi encaminhado por %s e aguarda triagem.", patientName, forwardedByName))
                    .severity("INFO")
                    .correlationId(correlationId)
                    .patientName(patientName)
                    .patientId(patientId)
                    .targetRoles(List.of("ENFERMEIRO"))
                    .build();
            eventProducer.publish(notification);
            log.info("Evento PACIENTE_ENCAMINHADO encaminhado. correlationId={} patientId={}", correlationId, patientId);
            return;
        }

        String classification = String.valueOf(event.getOrDefault("riskClassification", ""));
        Notification notification = Notification.builder()
                .id(eventId.isBlank() ? UUID.randomUUID().toString() : eventId)
                .type("TRIAGE_COMPLETED")
                .title("Triagem concluída")
                .message(String.format("Triagem de %s concluída. Classificação: %s", patientName, classification))
                .severity(isCritical(classification) ? "CRITICAL" : "INFO")
                .correlationId(correlationId)
                .patientName(patientName)
                .patientId(patientId)
                .triageId(triageId)
                .targetRoles(List.of("MEDICO"))
                .build();
        eventProducer.publish(notification);
        log.info("Evento de triagem encaminhado ao Kafka para notificacao. correlationId={}", correlationId);
    }

    @KafkaListener(topics = NotificationEventProducer.NOTIFICATIONS_TOPIC, groupId = "notification-broadcast-group")
    public void consumeNotification(Map<String, Object> payload) {
        Notification notification = objectMapper.convertValue(payload, Notification.class);

        Notification saved = notificationTimelineService.register(notification).orElse(null);
        if (saved == null) {
            log.warn("Notificacao duplicada descartada. id={}", notification.getId());
            return;
        }

        List<String> targetRoles = saved.getTargetRoles();
        boolean isTargeted = targetRoles != null && !targetRoles.isEmpty();

        if (!isTargeted) {
            messagingTemplate.convertAndSend("/topic/notifications", saved);
            if ("CRITICAL".equals(saved.getSeverity())) {
                messagingTemplate.convertAndSend("/topic/alerts", saved);
                log.warn("Alerta critico broadcast: {}", saved.getMessage());
            }
            return;
        }

        for (String role : targetRoles) {
            String roleSlug = role.toLowerCase(Locale.ROOT);
            messagingTemplate.convertAndSend("/topic/notifications/" + roleSlug, saved);
            if ("CRITICAL".equals(saved.getSeverity())) {
                messagingTemplate.convertAndSend("/topic/alerts/" + roleSlug, saved);
                log.warn("Alerta critico direcionado a {}: {}", roleSlug, saved.getMessage());
            }
        }
    }

    private boolean isCritical(String classification) {
        return "VERMELHO".equals(classification) || "LARANJA".equals(classification);
    }
}
