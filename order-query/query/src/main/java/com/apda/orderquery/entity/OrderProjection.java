package com.apda.orderquery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Read model per Order (versi 1 desain t2.1): snapshot proyeksi versi terbaru
 * atau "tombstone" yang memblok event kadaluwarsa setelah delete.
 * response_json hanya memuat bentuk ResponseTemplate publik
 * ({order, produk, pelanggan}).
 */
@Entity
@Table(name = "order_projection")
public class OrderProjection {

    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "owner_subject", length = 255)
    private String ownerSubject;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderProjection() {
    }

    // Snapshot aktif: versi + ResponseTemplate JSON + pemilik (internal).
    public static OrderProjection snapshot(Long orderId, long aggregateVersion, String ownerSubject,
            String responseJson, Instant updatedAt) {
        OrderProjection projection = new OrderProjection();
        projection.orderId = orderId;
        projection.aggregateVersion = aggregateVersion;
        projection.deleted = false;
        projection.ownerSubject = ownerSubject;
        projection.responseJson = responseJson;
        projection.updatedAt = updatedAt;
        return projection;
    }

    // Tombstone: tetap menyimpan-versi terakhir sebagai "version fence".
    public static OrderProjection tombstone(Long orderId, long aggregateVersion, Instant updatedAt) {
        OrderProjection projection = new OrderProjection();
        projection.orderId = orderId;
        projection.aggregateVersion = aggregateVersion;
        projection.deleted = true;
        projection.updatedAt = updatedAt;
        return projection;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
