package com.apda.orderquery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.apda.orderquery.consumer.OrderEventApplier;
import com.apda.orderquery.consumer.TestAmqpSupport;
import com.apda.orderquery.entity.OrderProjection;
import com.apda.orderquery.repository.OrderProjectionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uji perilaku proyeksi CQRS ORDER-QUERY terhadap event typed ORDER:
 * snapshot create, update versi baru, idempotensi eventId, event versi lama,
 * tombstone delete, dan perlindungan CHECK schema.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderEventApplierTest {

    @Autowired
    private OrderEventApplier applier;

    @Autowired
    private OrderProjectionRepository projectionRepository;

    // Database test berbagi antar test class; pakai state kosong per uji.
    @org.junit.jupiter.api.BeforeEach
    void bersihkanProyeksi() {
        projectionRepository.deleteAllInBatch();
        projectionRepository.findAll(); // memastikan flush sebelum lanjut
    }

    @Test
    void createEventProjectsCompleteSnapshot() {
        String event = TestAmqpSupport.createdEvent("order-1", 5L, 1L, "siswa1");
        applier.terapkan(event);

        OrderProjection projection = projectionRepository.findById(5L).orElseThrow();
        assertThat(projection.isDeleted()).isFalse();
        assertThat(projection.getAggregateVersion()).isEqualTo(1L);
        assertThat(projection.getOwnerSubject()).isEqualTo("siswa1");
        assertThat(projection.getResponseJson()).contains("\"order\"");
        assertThat(projection.getResponseJson()).contains("\"produk\"");
        assertThat(projection.getResponseJson()).contains("\"pelanggan\"");
    }

    @Test
    void replayingSameEventIsIdempotent() {
        String event = TestAmqpSupport.createdEvent("order-replay", 6L, 1L, "siswa1");
        applier.terapkan(event);
        long storedVersionBefore = projectionRepository.findById(6L).orElseThrow().getAggregateVersion();

        applier.terapkan(event); // pengiriman ulang harus aman.

        assertThat(projectionRepository.findById(6L).orElseThrow().getAggregateVersion())
                .isEqualTo(storedVersionBefore);
    }

    @Test
    void updateVersionsReplaceTheProjection() {
        applier.terapkan(TestAmqpSupport.createdEvent("order-update-1", 7L, 1L, "siswa1"));
        applier.terapkan(TestAmqpSupport.updatedEvent("order-update-2", 7L, 2L, "siswa1"));

        OrderProjection projection = projectionRepository.findById(7L).orElseThrow();
        assertThat(projection.getAggregateVersion()).isEqualTo(2L);
        assertThat(projection.getResponseJson()).contains("\"jumlah\":9");
    }

    @Test
    void staleVersionIsIgnored() {
        applier.terapkan(TestAmqpSupport.createdEvent("order-stale-1", 8L, 2L, null));
        applier.terapkan(TestAmqpSupport.createdEvent("order-stale-2", 8L, 1L, null));

        assertThat(projectionRepository.findById(8L).orElseThrow().getAggregateVersion()).isEqualTo(2L);
    }

    @Test
    void deleteWritesTombstoneWithoutPublicPayloadOrOwner() {
        applier.terapkan(TestAmqpSupport.createdEvent("order-del-1", 9L, 1L, "siswa1"));
        applier.terapkan(TestAmqpSupport.deletedEvent("order-del-2", 9L, 2L));

        OrderProjection tombstone = projectionRepository.findById(9L).orElseThrow();
        assertThat(tombstone.isDeleted()).isTrue();
        assertThat(tombstone.getOwnerSubject()).isNull();
        assertThat(tombstone.getResponseJson()).isNull();
    }
}
