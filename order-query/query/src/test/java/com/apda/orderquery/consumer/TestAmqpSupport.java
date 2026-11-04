package com.apda.orderquery.consumer;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pembuatan payload event ORDER_* untuk pengujian (strict bentuknya hampir
 * sama dengan factory di ORDER service sehingga applier nyata).
 */
public final class TestAmqpSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String createdEvent(String eventId, Long aggregateId, long aggregateVersion, String owner) {
        return build(eventId, "ORDER_CREATED", aggregateId, aggregateVersion, owner,
                orderBody(aggregateId, 1), produkBody(1), pelangganBody(2));
    }

    public static String updatedEvent(String eventId, Long aggregateId, long aggregateVersion, String owner) {
        return build(eventId, "ORDER_UPDATED", aggregateId, aggregateVersion, owner,
                orderBody(aggregateId, 9), produkBody(1), pelangganBody(2));
    }

    public static String deletedEvent(String eventId, Long aggregateId, long aggregateVersion) {
        return build(eventId, "ORDER_DELETED", aggregateId, aggregateVersion, null, null, null, null);
    }

    public static String build(String eventId, String eventType, Long aggregateId, long aggregateVersion,
            String ownerSubject, Map<String, Object> order, Map<String, Object> produk, Map<String, Object> pelanggan) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("schemaVersion", 1);
        envelope.put("aggregateType", "Order");
        envelope.put("aggregateId", aggregateId);
        envelope.put("aggregateVersion", aggregateVersion);
        envelope.put("occurredAt", "2026-10-30T08:00:00Z");
        envelope.put("ownerSubject", ownerSubject);
        envelope.put("order", order);
        envelope.put("produk", produk);
        envelope.put("pelanggan", pelanggan);
        try {
            return OBJECT_MAPPER.writeValueAsString(envelope);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Gagal serialisasi event uji", exception);
        }
    }

    private static Map<String, Object> orderBody(Long aggregateId, int jumlah) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", aggregateId);
        body.put("produkId", 1L);
        body.put("pelangganId", 2L);
        body.put("tglTrans", "2026-10-30");
        body.put("jumlah", jumlah);
        body.put("total", 50000);
        return body;
    }

    private static Map<String, Object> produkBody(long id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("nama", "Laptop");
        body.put("harga", 12000000);
        body.put("deskripsi", "Event-time snapshot");
        return body;
    }

    private static Map<String, Object> pelangganBody(long id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("nama", "Amel");
        body.put("alamat", "Padang");
        body.put("jenis_kelamin", "P");
        return body;
    }

    private TestAmqpSupport() {
    }
}
