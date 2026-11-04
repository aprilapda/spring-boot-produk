package com.apda.order.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

class OrderJsonMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeCamelCaseAndSnakeCaseJson() throws Exception {
        String camelCaseJson = "{\"produkId\":1,\"pelangganId\":2,\"tglTrans\":\"2026-10-02\",\"jumlah\":3,\"total\":1500000.00}";
        Order camelCaseOrder = objectMapper.readValue(camelCaseJson, Order.class);

        assertEquals(1L, camelCaseOrder.getProdukId());
        assertEquals(2L, camelCaseOrder.getPelangganId());
        assertEquals(LocalDate.of(2026, 10, 2), camelCaseOrder.getTglTrans());
        assertEquals(3, camelCaseOrder.getJumlah());
        assertEquals(new BigDecimal("1500000.00"), camelCaseOrder.getTotal());

        String snakeCaseJson = "{\"produk_id\":1,\"pelanggan_id\":2,\"tgl_trans\":\"2026-10-02\",\"jumlah\":3,\"total\":1500000.00}";
        Order snakeCaseOrder = objectMapper.readValue(snakeCaseJson, Order.class);

        assertEquals(1L, snakeCaseOrder.getProdukId());
        assertEquals(2L, snakeCaseOrder.getPelangganId());
        assertEquals(LocalDate.of(2026, 10, 2), snakeCaseOrder.getTglTrans());
        assertEquals(3, snakeCaseOrder.getJumlah());
        assertEquals(new BigDecimal("1500000.00"), snakeCaseOrder.getTotal());
    }
}
