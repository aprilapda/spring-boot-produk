package com.apda.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Outbox event untuk sinkronisasi CQRS. Ditulis pada transaksi yang sama
 * dengan perubahan Order (command), lalu terbitkan oleh dispatcher tersendiri
 * sehingga kegagalan koneksi RabbitMQ tidak membuat event hilang.
 */
@Entity
@Table(name = "order_event_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id"),
        @UniqueConstraint(name = "uk_outbox_aggregate_version",
                columnNames = {"aggregate_id", "aggregate_version"})
})
public class OrderEventOutbox {

    // Event dihasilkan dengan UUID di command; hanya terbit satu kali per event.
    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "delivered", nullable = false)
    private boolean delivered;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected OrderEventOutbox() {
    }

    public OrderEventOutbox(String eventId, String eventType, Long aggregateId, long aggregateVersion,
            Instant occurredAt, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.delivered = false;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void markDelivered() {
        this.delivered = true;
        this.deliveredAt = Instant.now();
    }
}
