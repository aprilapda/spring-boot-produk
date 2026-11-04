package com.apda.order.event;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.apda.order.entity.OrderEventOutbox;
import com.apda.order.repository.OrderEventOutboxRepository;

/**
 * Dispatcher outbox: menerbitkan event yang tersimpan ke RabbitMQ secara
 * berulang. Order tersimpan tetap valid walaupun RabbitMQ sedang mati; event
 * coba dikirim lagi pada siklus berikutnya (retry tanpa batas, sesuai desain).
 */
@Component
public class OrderEventOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventOutboxDispatcher.class);

    private final OrderEventOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderEventOutboxDispatcher(OrderEventOutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void terbitkanEventPending() {
        List<OrderEventOutbox> pending = outboxRepository.findByDeliveredFalseOrderByOccurredAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        for (OrderEventOutbox event : pending) {
            try {
                CorrelationData correlation = new CorrelationData(event.getEventId());
                rabbitTemplate.convertAndSend(OrderEventConstants.EXCHANGE,
                        OrderEventConstants.routingKeyFor(event.getEventType()), event.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck()) {
                    throw new IllegalStateException("RabbitMQ menolak event: " + confirm.getReason());
                }
                if (correlation.getReturned() != null) {
                    throw new IllegalStateException("Event tidak memiliki antrean tujuan yang terikat");
                }
                event.markDelivered();
                outboxRepository.save(event);
                log.info("Outbox event {} ({}) terkirim untuk order #{} v{}",
                        event.getEventId(), event.getEventType(), event.getAggregateId(),
                        event.getAggregateVersion());
            } catch (RuntimeException exception) {
                // RabbitMQ tidak siap: hentikan putaran ini, event dicoba lagi nanti.
                log.warn("Outbox event {} tertunda, broker tidak siap: {}", event.getEventId(), exception.getMessage());
                break;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("Pengiriman outbox event {} dihentikan", event.getEventId());
                break;
            } catch (ExecutionException | TimeoutException exception) {
                log.warn("Konfirmasi RabbitMQ belum diterima untuk event {}: {}",
                        event.getEventId(), exception.getMessage());
                break;
            }
        }
    }
}
