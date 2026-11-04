package com.apda.order.config;

import static com.apda.order.event.OrderEventConstants.EMAIL_DLQ;
import static com.apda.order.event.OrderEventConstants.EMAIL_QUEUE;
import static com.apda.order.event.OrderEventConstants.EXCHANGE;
import static com.apda.order.event.OrderEventConstants.KEY_CREATED;
import static com.apda.order.event.OrderEventConstants.KEY_DELETED;
import static com.apda.order.event.OrderEventConstants.KEY_UPDATED;
import static com.apda.order.event.OrderEventConstants.PROJECTION_DLQ;
import static com.apda.order.event.OrderEventConstants.PROJECTION_QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologi RabbitMQ untuk CQRS Order (desain t2/t2.1):
 * exchange topic `abl.order.events` dengan routing key order.created /
 * order.updated / order.deleted; dua antrean durable (proyeksi & email) yang
 * masing-masing punya DLQ sendiri. Antrean lama `emailQueue` tetap dipakai
 * khusus pesan teks dari Producer endpoint /email/send (tidak diganti).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Antrean proyeksi ORDER-QUERY: semua tipe event.
    @Bean
    public Queue orderProjectionQueue() {
        return QueueBuilder.durable(PROJECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PROJECTION_DLQ)
                .build();
    }

    @Bean
    public Queue orderProjectionDlq() {
        return QueueBuilder.durable(PROJECTION_DLQ).build();
    }

    @Bean
    public Binding orderCreatedToProjection() {
        return BindingBuilder.bind(orderProjectionQueue()).to(orderEventsExchange()).with(KEY_CREATED);
    }

    @Bean
    public Binding orderUpdatedToProjection() {
        return BindingBuilder.bind(orderProjectionQueue()).to(orderEventsExchange()).with(KEY_UPDATED);
    }

    @Bean
    public Binding orderDeletedToProjection() {
        return BindingBuilder.bind(orderProjectionQueue()).to(orderEventsExchange()).with(KEY_DELETED);
    }

    // Antrean notifikasi email: hanya event CREATED.
    @Bean
    public Queue emailOrderCreatedQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue emailOrderCreatedDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding orderCreatedToEmail() {
        return BindingBuilder.bind(emailOrderCreatedQueue()).to(orderEventsExchange()).with(KEY_CREATED);
    }
}
