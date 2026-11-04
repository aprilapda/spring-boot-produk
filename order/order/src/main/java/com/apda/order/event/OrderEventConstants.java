package com.apda.order.event;

// Konstanta topologi RabbitMQ CQRS Order.
// Salinan yang konsisten dipakai oleh ORDER (penerbit), ORDER-QUERY (proyeksi),
// dan CONSUMER (notifikasi email). Jangan ubah hanya di satu tempat.
public final class OrderEventConstants {

    public static final String EXCHANGE = "abl.order.events";
    public static final String KEY_CREATED = "order.created";
    public static final String KEY_UPDATED = "order.updated";
    public static final String KEY_DELETED = "order.deleted";

    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_UPDATED = "ORDER_UPDATED";
    public static final String EVENT_ORDER_DELETED = "ORDER_DELETED";

    public static final String PROJECTION_QUEUE = "abl.order.projection";
    public static final String PROJECTION_DLQ = "abl.order.projection.dlq";
    public static final String EMAIL_QUEUE = "abl.email.order-created";
    public static final String EMAIL_DLQ = "abl.email.order-created.dlq";

    public static final int SCHEMA_VERSION = 1;
    public static final String AGGREGATE_TYPE = "Order";

    private OrderEventConstants() {
    }

    // Mapping tipe event -> routing key antrean exchange topic.
    public static String routingKeyFor(String eventType) {
        if (EVENT_ORDER_CREATED.equals(eventType)) {
            return KEY_CREATED;
        }
        if (EVENT_ORDER_UPDATED.equals(eventType)) {
            return KEY_UPDATED;
        }
        if (EVENT_ORDER_DELETED.equals(eventType)) {
            return KEY_DELETED;
        }
        throw new IllegalArgumentException("Tipe event tidak dikenal: " + eventType);
    }
}
