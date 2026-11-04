package com.apda.order.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.apda.order.entity.OrderEventOutbox;
import com.apda.order.repository.OrderEventOutboxRepository;

class OrderEventOutboxDispatcherTest {

    private final OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OrderEventOutboxDispatcher dispatcher =
            new OrderEventOutboxDispatcher(repository, rabbitTemplate);

    @Test
    void marksOutboxDeliveredOnlyAfterBrokerAcknowledgesEvent() {
        OrderEventOutbox event = pendingEvent();
        when(repository.findByDeliveredFalseOrderByOccurredAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, "ack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(String.class),
                any(CorrelationData.class));

        dispatcher.terbitkanEventPending();

        assertTrue(event.isDelivered());
        verify(repository).save(event);
    }

    @Test
    void leavesOutboxPendingWhenBrokerRejectsEvent() {
        OrderEventOutbox event = pendingEvent();
        when(repository.findByDeliveredFalseOrderByOccurredAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(String.class),
                any(CorrelationData.class));

        dispatcher.terbitkanEventPending();

        assertFalse(event.isDelivered());
        verify(repository, never()).save(event);
    }

    private OrderEventOutbox pendingEvent() {
        return new OrderEventOutbox("event-1", OrderEventConstants.EVENT_ORDER_CREATED, 12L, 1L,
                Instant.now(), "{\"eventId\":\"event-1\"}");
    }
}
