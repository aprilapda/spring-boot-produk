package com.apda.consumer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologi RabbitMQ sisi Consumer:
 * - Antrean lama `emailQueue` tetap ada untuk pesan teks dari Producer.
 * - Antrean typed `abl.email.order-created` (event ORDER_CREATED dari ORDER)
 *   dengan DLQ, sesuai desain t2.1.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(OrderEventConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue emailOrderCreatedQueue() {
        return QueueBuilder.durable(OrderEventConstants.EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", OrderEventConstants.EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue emailOrderCreatedDlq() {
        return QueueBuilder.durable(OrderEventConstants.EMAIL_DLQ).build();
    }

    @Bean
    public Binding orderCreatedToEmail() {
        return BindingBuilder.bind(emailOrderCreatedQueue())
                .to(orderEventsExchange())
                .with(OrderEventConstants.KEY_CREATED);
    }

    // Antrean lama untuk endpoint /email/send milik Producer.
    @Bean
    Queue emailQueueLegacy() {
        return new Queue("emailQueue", true);
    }

    @Bean
    Queue emailQueueLegacyDlq() {
        return new Queue("emailQueue.dlq", true);
    }
}
