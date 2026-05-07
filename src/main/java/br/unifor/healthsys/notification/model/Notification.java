package br.unifor.healthsys.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;
    private String type;
    private String title;
    private String message;
    private String severity;
    private Long patientId;
    private String patientName;
    private Long triageId;
    @Indexed
    private String correlationId;
    @Builder.Default
    private List<String> targetRoles = new ArrayList<>();
    @Indexed
    private LocalDateTime timestamp;

    public static Notification fromEvent(java.util.Map<String, Object> event) {
        String type = String.valueOf(event.getOrDefault("type", "INFO"));
        String severity = type.equals("EMERGENCY_ALERT") ? "CRITICAL" : "INFO";

        return Notification.builder()
                .type(type)
                .title(severity.equals("CRITICAL") ? "ALERTA DE EMERGENCIA" : "Notificacao")
                .message(String.valueOf(event.getOrDefault("message", "")))
                .severity(severity)
                .patientId(event.get("patientId") instanceof Number number ? number.longValue() : null)
                .patientName(String.valueOf(event.getOrDefault("patientName", "")))
                .correlationId(String.valueOf(event.getOrDefault("correlationId", "")))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
