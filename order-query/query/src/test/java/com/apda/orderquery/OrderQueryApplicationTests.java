package com.apda.orderquery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderQueryApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    // Database test H2 in-memory dipakai berbagi antar test class (context cache);
    // bersihkan agar uji skema selalu mulai dari tabel kosong.
    @org.junit.jupiter.api.BeforeEach
    void bersihkanTabel() {
        jdbcTemplate.update("DELETE FROM processed_order_event");
        jdbcTemplate.update("DELETE FROM order_projection");
    }

    @Test
    void startsWithTheIndependentProjectionAndInboxSchema() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_projection", Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_order_event", Integer.class
        )).isZero();
    }

    @Test
    void recordsTombstonesAsVersionFencesWithoutPublicPayloadOrOwner() {
        jdbcTemplate.update(
                "INSERT INTO order_projection "
                        + "(order_id, aggregate_version, is_deleted, owner_subject, response_json, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                91L, 3L, true, null, null
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT aggregate_version FROM order_projection WHERE order_id = 91", Long.class
        )).isEqualTo(3L);
    }

    @Test
    void rejectsConflictingInboxEventsForOneAggregateVersion() {
        jdbcTemplate.update(
                "INSERT INTO processed_order_event "
                        + "(event_id, aggregate_id, aggregate_version, event_type, processed_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "00000000-0000-0000-0000-000000000001", 12L, 1L, "ORDER_CREATED"
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO processed_order_event "
                        + "(event_id, aggregate_id, aggregate_version, event_type, processed_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "00000000-0000-0000-0000-000000000002", 12L, 1L, "ORDER_UPDATED"
        )).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void rejectsOrderReadsWithoutAValidJwt() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/999").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsJwtAndHonorsProjectionVersionHeaderFromWriteResponse() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO order_projection "
                        + "(order_id, aggregate_version, is_deleted, owner_subject, response_json, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                92L, 3L, true, null, null
        );

        mockMvc.perform(get("/api/orders/92")
                        .header("Authorization", "Bearer " + validToken("siswa1", "ADMIN_PELANGGAN"))
                        .header("X-Order-Projection-Min-Version", "3"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders/92")
                        .header("Authorization", "Bearer " + validToken("siswa1", "ADMIN_PELANGGAN"))
                        .header("X-Order-Projection-Min-Version", "4"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void usersCanOnlyReadTheirOwnOrdersWhileAdminCanReadAll() throws Exception {
        insertActiveProjection(93L, "siswa1");
        insertActiveProjection(94L, "siswa2");

        mockMvc.perform(get("/api/orders/93")
                        .header("Authorization", "Bearer " + validToken("siswa1", "USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/94")
                        .header("Authorization", "Bearer " + validToken("siswa1", "USER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + validToken("siswa1", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].order.id").value(93));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + validToken("admin", "ADMIN_PELANGGAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    private void insertActiveProjection(Long id, String ownerSubject) {
        jdbcTemplate.update(
                "INSERT INTO order_projection "
                        + "(order_id, aggregate_version, is_deleted, owner_subject, response_json, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id, 1L, false, ownerSubject,
                "{\"order\":{\"id\":" + id + "},\"produk\":null,\"pelanggan\":null}"
        );
    }

    private String validToken(String subject, String role) {
        String signingKey = "RahasiaBangetIniHarusPanjangMinimal32HurufUntukHS256-1234567890";
        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }
}
