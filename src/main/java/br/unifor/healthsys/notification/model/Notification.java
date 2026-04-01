package br.unifor.healthsys.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String id;
    private String type;
    private String title;
    private String message;
    private String severity;   // INFO, WARNING, CRITICAL
    private Long patientId;
    private String patientName;
    private LocalDateTime timestamp;

    public static Notification fromEvent(java.util.Map<String, Object> event) {
        String type = String.valueOf(event.getOrDefault("type", "INFO"));
        String severity = type.equals("EMERGENCY_ALERT") ? "CRITICAL" : "INFO";

        return Notification.builder()
                .type(type)
                .title(severity.equals("CRITICAL") ? "ALERTA DE EMERGENCIA" : "Notificacao")
                .message(String.valueOf(event.getOrDefault("message", "")))
                .severity(severity)
                .patientId(event.containsKey("patientId")
                        ? Long.parseLong(String.valueOf(event.get("patientId"))) : null)
                .patientName(String.valueOf(event.getOrDefault("patientName", "")))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
