package lk.swiftdrop.merchant.config;

import lk.swiftdrop.common.event.ParcelEvents;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Receives every ParcelStatusChanged event; the consumer ignores parcels that are not merchant orders. */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange parcelEventsExchange() {
        return new TopicExchange(ParcelEvents.EXCHANGE, true, false);
    }

    @Bean
    public Queue merchantOrdersQueue(@Value("${rabbitmq.queues.merchant-orders}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding merchantOrdersBinding(Queue merchantOrdersQueue, TopicExchange parcelEventsExchange) {
        return BindingBuilder.bind(merchantOrdersQueue).to(parcelEventsExchange).with(ParcelEvents.ALL_STATUS_CHANGES);
    }

    /** Same converter as notification-service: the event class comes from the message's type header. */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter("*");
    }
}
