package com.apda.orderquery.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apda.orderquery.entity.OrderProjection;
import com.apda.orderquery.repository.OrderProjectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Endpoint Query CQRS: GET /api/orders (list), /{id}, /{id}/details.
 * Path publik tetap sama dengan sebelumnya sesuai desain: gateway membagi
 * GET -> ORDER-QUERY dan POST/PUT/DELETE -> ORDER. Respons diambil dari
 * snapshot event-time (konsistensi eventual).
 *
 * Header opsional X-Min-Order-Version: jika proyeksi lebih lama, balas
 * 503 READ_MODEL_LAGGING (Retry-After: 1) sesuai kontrak t2.1.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    public static final String MIN_ORDER_VERSION_HEADER = "X-Min-Order-Version";
    public static final String ORDER_PROJECTION_MIN_VERSION_HEADER = "X-Order-Projection-Min-Version";

    private static final Logger log = LoggerFactory.getLogger(OrderQueryController.class);

    private final OrderProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderQueryController(OrderProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Object>> findAll(Authentication authentication) {
        List<OrderProjection> readableProjections = isAdministrator(authentication)
                ? projectionRepository.findAllByOrderByOrderIdAsc()
                : projectionRepository.findAllByOwnerSubjectOrderByOrderIdAsc(subjectOf(authentication));
        List<Object> projections = readableProjections.stream()
                .filter(projection -> !projection.isDeleted())
                .map(this::parseResponseJson)
                .toList();
        return ResponseEntity.ok(projections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> findById(@PathVariable Long id,
            @RequestHeader(name = MIN_ORDER_VERSION_HEADER, required = false) Long minVersion,
            @RequestHeader(name = ORDER_PROJECTION_MIN_VERSION_HEADER, required = false) Long projectionMinVersion,
            Authentication authentication) {
        return fetchProjection(id, stricterVersion(minVersion, projectionMinVersion), authentication);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<Object> findWithDetailsById(@PathVariable Long id,
            @RequestHeader(name = MIN_ORDER_VERSION_HEADER, required = false) Long minVersion,
            @RequestHeader(name = ORDER_PROJECTION_MIN_VERSION_HEADER, required = false) Long projectionMinVersion,
            Authentication authentication) {
        return fetchProjection(id, stricterVersion(minVersion, projectionMinVersion), authentication);
    }

    private Long stricterVersion(Long minVersion, Long projectionMinVersion) {
        if (minVersion == null) {
            return projectionMinVersion;
        }
        if (projectionMinVersion == null) {
            return minVersion;
        }
        return Math.max(minVersion, projectionMinVersion);
    }

    private ResponseEntity<Object> fetchProjection(Long id, Long minVersion, Authentication authentication) {
        OrderProjection existing = projectionRepository.findById(id).orElse(null);

        if (existing != null && !isAdministrator(authentication)
                && (!isReadableBy(existing, authentication) || existing.isDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // Dengan header konsistensi: belum-terproyeksi sama saja proyeksi
        // tertinggal (event mungkin masih di jalan) -> 503, bukan 404. Tombstone
        // tetap dibandingkan versinya agar delete yang sudah terproyeksi menjadi 404.
        if (minVersion != null && (existing == null || existing.getAggregateVersion() < minVersion)) {
            long projected = existing != null ? existing.getAggregateVersion() : 0L;
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Retry-After", "1")
                    .body(readModelLaggingBody(id, minVersion, projected));
        }
        if (existing == null || existing.isDeleted()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(parseResponseJson(existing));
    }

    private boolean isReadableBy(OrderProjection projection, Authentication authentication) {
        return authentication != null && authentication.getName().equals(projection.getOwnerSubject());
    }

    private String subjectOf(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN_PELANGGAN"::equals);
    }

    private Object readModelLaggingBody(Long id, Long minVersion, long projectedVersion) {
        return Map.of(
                "error", "READ_MODEL_LAGGING",
                "orderId", id,
                "minimumVersion", minVersion,
                "projectedVersion", projectedVersion);
    }

    private Object parseResponseJson(OrderProjection projection) {
        try {
            return objectMapper.readValue(projection.getResponseJson(), Object.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            // Snapshot korup secara teori; kembalikan mentahnya supaya datanya tetap terlihat.
            log.warn("response_json tidak bisa di-parse untuk order #{}: {}", projection.getOrderId(),
                    exception.getMessage());
            return projection.getResponseJson();
        }
    }
}
