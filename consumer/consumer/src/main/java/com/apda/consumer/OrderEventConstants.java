package com.apda.consumer;

// Salinan konstanta topologi RabbitMQ CQRS ORDER (harus identik dengan versi
// di ORDER dan ORDER-QUERY).
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
}
