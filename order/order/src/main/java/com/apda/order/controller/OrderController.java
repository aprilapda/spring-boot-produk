package com.apda.order.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apda.order.entity.Order;
import com.apda.order.service.OrderService;
import com.apda.order.vo.ResponseTemplate;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public List<ResponseTemplate> findAll(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			Authentication authentication) {
		return orderService.findAllWithDetails(authorization, subjectOf(authentication), isAdministrator(authentication));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ResponseTemplate> findById(@PathVariable Long id,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			Authentication authentication) {
		return orderService.findWithDetailsById(id, authorization, subjectOf(authentication),
				isAdministrator(authentication))
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}/details")
	public ResponseEntity<ResponseTemplate> findWithDetailsById(@PathVariable Long id,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			Authentication authentication) {
		return orderService.findWithDetailsById(id, authorization, subjectOf(authentication),
				isAdministrator(authentication))
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// CQRS: perubahan membawa header baca model (versi minimal proyeksi).
	private static ResponseEntity.BodyBuilder withConsistencyHeaders(Order order) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.header("X-Read-Model-Consistency", "eventual")
				.header("X-Order-Projection-Min-Version", String.valueOf(order.getAggregateVersion()));
	}

	@PostMapping
	public ResponseEntity<Order> create(@RequestBody Order order,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			Authentication authentication) {
		Order savedOrder = orderService.save(order, authorization, subjectOf(authentication));
		return withConsistencyHeaders(savedOrder).body(savedOrder);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			Authentication authentication) {
		return orderService.update(id, order, authorization, subjectOf(authentication),
				isAdministrator(authentication))
				.map(updated -> ResponseEntity.status(HttpStatus.OK)
						.header("X-Read-Model-Consistency", "eventual")
						.header("X-Order-Projection-Min-Version",
								String.valueOf(updated.getAggregateVersion()))
						.body(updated))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		return orderService.deleteById(id, subjectOf(authentication), isAdministrator(authentication))
				.map(deletionVersion -> ResponseEntity.noContent()
						.header("X-Read-Model-Consistency", "eventual")
						.header("X-Order-Projection-Min-Version", String.valueOf(deletionVersion))
						.<Void>build())
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// Pemilik order diambil dari sub token (sesuai desain: tidak dari body request).
	private static String subjectOf(Authentication authentication) {
		return authentication != null ? authentication.getName() : null;
	}

	private static boolean isAdministrator(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN_PELANGGAN".equals(authority.getAuthority()));
	}
}
