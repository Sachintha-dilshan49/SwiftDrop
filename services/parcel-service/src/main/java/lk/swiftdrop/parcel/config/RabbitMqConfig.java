package lk.swiftdrop.parcel.config;

import lk.swiftdrop.common.event.ParcelEvents;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.queues.tracking:tracking.q}")
    private String trackingQueue;

    @Bean
    public TopicExchange parcelEventsExchange() {
        return new TopicExchange(ParcelEvents.EXCHANGE, true, false);
    }

    @Bean
    public Queue trackingQueue() {
        return QueueBuilder.durable(trackingQueue).build();
    }

    @Bean
    public Binding trackingBinding() {
        return BindingBuilder.bind(trackingQueue())
                .to(parcelEventsExchange())
                .with(ParcelEvents.ALL_STATUS_CHANGES);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "*");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        // Unroutable messages come back to the outbox relay instead of being dropped silently
        template.setMandatory(true);
        return template;
    }
}
