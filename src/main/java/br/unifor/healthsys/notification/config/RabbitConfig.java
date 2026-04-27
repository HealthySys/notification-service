package br.unifor.healthsys.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange("notifications.exchange");
    }

    @Bean
    public DirectExchange notificationsDlx() {
        return new DirectExchange("notifications.dlx");
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue("notifications.queue", true, false, false, Map.of(
                "x-dead-letter-exchange", "notifications.dlx"
        ));
    }

    @Bean
    public Queue notificationsDlq() {
        return new Queue("notifications.dlq", true);
    }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(notificationsExchange())
                .with("notifications");
    }

    @Bean
    public Binding notificationsDlqBinding() {
        return BindingBuilder.bind(notificationsDlq())
                .to(notificationsDlx())
                .with("notifications");
    }
}
