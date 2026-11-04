package com.apda.orderquery.consumer;

import static com.apda.orderquery.event.OrderEventConstants.PROJECTION_QUEUE;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener antrean proyeksi `abl.order.projection`. Pesan hanya dialui ke
 * applier yang transaksional; konsumen diack otomatis setelah metode selesai.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderEventApplier applier;

    public OrderEventConsumer(OrderEventApplier applier) {
        this.applier = applier;
    }

    @RabbitListener(queues = PROJECTION_QUEUE)
    public void terimaEvent(String payload) {
        try {
            applier.terapkan(payload);
        } catch (RuntimeException exception) {
            log.error("Gagal mengaplikasi event proyeksi (multipartende ke DLQ jika retry habis): {}",
                    exception.getMessage());
            throw exception;
        }
    }
}
