package br.unifor.healthsys.notification.messaging;

import br.unifor.healthsys.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventProducer {

    public static final String NOTIFICATIONS_TOPIC = "notifications-events";

    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Notification notification) {
        String key = notification.getCorrelationId() != null && !notification.getCorrelationId().isBlank()
                ? notification.getCorrelationId()
                : notification.getId();
        kafkaTemplate.send(NOTIFICATIONS_TOPIC, key, notification)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar notificacao no Kafka. id={} correlationId={}",
                                notification.getId(), notification.getCorrelationId(), ex);
                    } else {
                        log.info("Notificacao publicada no Kafka. id={} correlationId={} severity={}",
                                notification.getId(), notification.getCorrelationId(), notification.getSeverity());
                    }
                });
    }
}
