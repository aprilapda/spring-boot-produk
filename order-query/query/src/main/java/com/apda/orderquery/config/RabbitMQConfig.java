package com.apda.orderquery.config;

import static com.apda.orderquery.event.OrderEventConstants.EXCHANGE;
import static com.apda.orderquery.event.OrderEventConstants.KEY_CREATED;
import static com.apda.orderquery.event.OrderEventConstants.KEY_DELETED;
import static com.apda.orderquery.event.OrderEventConstants.KEY_UPDATED;
import static com.apda.orderquery.event.OrderEventConstants.PROJECTION_DLQ;
import static com.apda.orderquery.event.OrderEventConstants.PROJECTION_QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deklarasi objek RabbitMQ sisi ORDER-QUERY (harus identik dengan deklarasi
 * ORDER): exchange topic abl.order.events + antrean proyeksi durable dengan
 * DLQ. Pesan gagal permanen dipindah ke abl.order.projection.dlq.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

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
}
