package com.apda.order.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.apda.order.entity.Order;
import com.apda.order.vo.Pelanggan;
import com.apda.order.vo.Produk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Membuat JSON envelope event ORDER_* sesuai kontrak t2.1:
 * field stabil (schemaVersion, eventType, aggregateType, aggregateId,
 * aggregateVersion, occurredAt, ownerSubject) + snapshot lengkap
 * order/produk/pelanggan (event-time). Field owner/order disembunyikan dari
 * JSON publik oleh anotasi entity, sehingga envelope aman dipakai consumer.
 */
@Component
public class OrderEventEnvelopeFactory {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String serialize(String eventId, String eventType, Long aggregateId, long aggregateVersion,
            String ownerSubject, Order order, Produk produk, Pelanggan pelanggan) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", eventId);
            envelope.put("eventType", eventType);
            envelope.put("schemaVersion", OrderEventConstants.SCHEMA_VERSION);
            envelope.put("aggregateType", OrderEventConstants.AGGREGATE_TYPE);
            envelope.put("aggregateId", aggregateId);
            envelope.put("aggregateVersion", aggregateVersion);
            envelope.put("occurredAt", Instant.now().toString());
            envelope.put("ownerSubject", ownerSubject);
            envelope.put("order", order != null ? objectMapper.convertValue(order, Map.class) : null);
            envelope.put("produk", produk != null ? objectMapper.convertValue(produk, Map.class) : null);
            envelope.put("pelanggan", pelanggan != null ? objectMapper.convertValue(pelanggan, Map.class) : null);
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gagal membuat JSON event order", exception);
        }
    }
}
