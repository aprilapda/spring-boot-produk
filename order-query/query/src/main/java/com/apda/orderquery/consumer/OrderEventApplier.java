package com.apda.orderquery.consumer;

import static com.apda.orderquery.event.OrderEventConstants.AGGREGATE_TYPE;
import static com.apda.orderquery.event.OrderEventConstants.EVENT_ORDER_CREATED;
import static com.apda.orderquery.event.OrderEventConstants.EVENT_ORDER_DELETED;
import static com.apda.orderquery.event.OrderEventConstants.EVENT_ORDER_UPDATED;
import static com.apda.orderquery.event.OrderEventConstants.PROJECTION_QUEUE;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apda.orderquery.entity.OrderProjection;
import com.apda.orderquery.entity.ProcessedOrderEvent;
import com.apda.orderquery.repository.OrderProjectionRepository;
import com.apda.orderquery.repository.ProcessedOrderEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Menerapkan event Order ke proyeksi CQRS (read model).
 * - Idempoten: eventId sudah diproses -> diabaikan (ack, tanpa efek).
 * - Versi kecil = event lama -> diabaikan; versi sama dengan eventId beda
 *   -> ditolak ke DLQ (konflik versi).
 * - Inbox + mutasi proyeksi di-commit bersama (desain t2.1).
 */
@Service
public class OrderEventApplier {

    private final OrderProjectionRepository projectionRepository;
    private final ProcessedOrderEventRepository processedEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventApplier(OrderProjectionRepository projectionRepository,
            ProcessedOrderEventRepository processedEventRepository) {
        this.projectionRepository = projectionRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void terapkan(String payload) {
        JsonNode envelope = parse(payload);
        String eventId = envelope.path("eventId").asText(null);
        String eventType = envelope.path("eventType").asText(null);
        long aggregateId = envelope.path("aggregateId").asLong(-1L);
        long aggregateVersion = envelope.path("aggregateVersion").asLong(-1L);
        int schemaVersion = envelope.path("schemaVersion").asInt(-1);
        String aggregateType = envelope.path("aggregateType").asText(null);

        if (eventId == null || eventId.isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Event tanpa eventId; tidak dapat diproses");
        }
        if (schemaVersion != 1 || !AGGREGATE_TYPE.equals(aggregateType)) {
            throw new AmqpRejectAndDontRequeueException(
                    "Event " + eventId + " tidak kompatibel (schemaVersion/aggregateType)");
        }
        if (aggregateId <= 0L || aggregateVersion <= 0L) {
            throw new AmqpRejectAndDontRequeueException("Event " + eventId + " punya identitas agregat tidak valid");
        }
        if (!isKnownEventType(eventType)) {
            throw new AmqpRejectAndDontRequeueException("Tipe event tidak dikenal: " + eventType);
        }

        if (processedEventRepository.existsById(eventId)) {
            return; // pengiriman ulang event yang sudah diproses.
        }

        OrderProjection existing = projectionRepository.findById(aggregateId).orElse(null);
        if (existing != null) {
            long storedVersion = existing.getAggregateVersion();
            if (storedVersion > aggregateVersion) {
                return; // event lebih lama dari proyeksi: ack tanpa mengubah state.
            }
            if (storedVersion == aggregateVersion) {
                throw new AmqpRejectAndDontRequeueException(
                        "Konflik versi: order #" + aggregateId + " v" + aggregateVersion
                                + " sudah direpresentasikan proyeksi");
            }
        }

        OrderProjection next;
        if (EVENT_ORDER_DELETED.equals(eventType)) {
            next = OrderProjection.tombstone(aggregateId, aggregateVersion, Instant.now());
        } else {
            JsonNode orderNode = envelope.get("order");
            if (orderNode == null || orderNode.isNull()) {
                throw new AmqpRejectAndDontRequeueException(
                        "Event " + eventType + " tanpa snapshot order; tidak dapat diproyeksikan");
            }
            Map<String, Object> responseTemplate = new LinkedHashMap<>();
            responseTemplate.put("order", orderNode);
            responseTemplate.put("produk", envelope.get("produk"));
            responseTemplate.put("pelanggan", envelope.get("pelanggan"));
            String responseJson;
            try {
                responseJson = objectMapper.writeValueAsString(responseTemplate);
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new AmqpRejectAndDontRequeueException("Gagal serialisasi snapshot proyeksi", exception);
            }
            String ownerSubject = envelope.path("ownerSubject").asText(null);
            next = OrderProjection.snapshot(aggregateId, aggregateVersion, ownerSubject, responseJson, Instant.now());
        }

        projectionRepository.save(next);
        processedEventRepository.save(new ProcessedOrderEvent(eventId, aggregateId, aggregateVersion,
                eventType, Instant.now()));
    }

    private boolean isKnownEventType(String eventType) {
        return EVENT_ORDER_CREATED.equals(eventType) || EVENT_ORDER_UPDATED.equals(eventType)
                || EVENT_ORDER_DELETED.equals(eventType);
    }

    private JsonNode parse(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AmqpRejectAndDontRequeueException("Payload event buruk, langsung ke DLQ", exception);
        }
    }
}
