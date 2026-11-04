package com.apda.orderquery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Inbox idempotensi per event. Di-commit pada transaksi yang sama dengan
 * mutasi proyeksi agar pengiriman at-least-once aman.
 */
@Entity
@Table(
        name = "processed_order_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_order_event_aggregate_version",
                columnNames = {"aggregate_id", "aggregate_version"}
        )
)
public class ProcessedOrderEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedOrderEvent() {
    }

    public ProcessedOrderEvent(String eventId, Long aggregateId, long aggregateVersion, String eventType,
            Instant processedAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
