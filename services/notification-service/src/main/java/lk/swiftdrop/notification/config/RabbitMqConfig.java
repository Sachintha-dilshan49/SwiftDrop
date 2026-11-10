package lk.swiftdrop.notification.config;

import lk.swiftdrop.common.event.ParcelEvents;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.queues.notification}")
    private String notificationQueue;

    @Value("${rabbitmq.queues.webhook}")
    private String webhookQueue;

    @Value("${rabbitmq.queues.webhook-retry-10s}")
    private String webhookRetry10s;

    @Value("${rabbitmq.queues.webhook-retry-1m}")
    private String webhookRetry1m;

    @Value("${rabbitmq.queues.webhook-retry-5m}")
    private String webhookRetry5m;

    @Value("${rabbitmq.queues.webhook-parking}")
    private String webhookParking;

    @Value("${rabbitmq.dlx.webhook}")
    private String webhookDlx;

    // ======== Exchanges ========

    @Bean
    public TopicExchange parcelEventsExchange() {
        return new TopicExchange(ParcelEvents.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange webhookDlxExchange() {
        return new DirectExchange(webhookDlx, true, false);
    }

    // ======== Main queues ========

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue).build();
    }

    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(webhookQueue).build();
    }

    // ======== Retry queues ========

    @Bean
    public Queue webhookRetry10sQueue() {
        return QueueBuilder.durable(webhookRetry10s)
                .ttl(10_000)
                .deadLetterExchange(webhookDlx)
                .deadLetterRoutingKey("retry")
                .build();
    }

    @Bean
    public Queue webhookRetry1mQueue() {
        return QueueBuilder.durable(webhookRetry1m)
                .ttl(60_000)
                .deadLetterExchange(webhookDlx)
                .deadLetterRoutingKey("retry")
                .build();
    }

    @Bean
    public Queue webhookRetry5mQueue() {
        return QueueBuilder.durable(webhookRetry5m)
                .ttl(300_000)
                .deadLetterExchange(webhookDlx)
                .deadLetterRoutingKey("retry")
                .build();
    }

    @Bean
    public Queue webhookParkingQueue() {
        return QueueBuilder.durable(webhookParking).build();
    }

    // ======== Bindings ========

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(parcelEventsExchange())
                .with(ParcelEvents.ALL_STATUS_CHANGES);
    }

    @Bean
    public Binding webhookBinding() {
        return BindingBuilder.bind(webhookQueue())
                .to(parcelEventsExchange())
                .with(ParcelEvents.ALL_STATUS_CHANGES);
    }

    @Bean
    public Binding webhookDlxBinding() {
        return BindingBuilder.bind(webhookQueue())
                .to(webhookDlxExchange())
                .with("retry");
    }

    // ======== Message converter ========

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter("*");
    }
}
