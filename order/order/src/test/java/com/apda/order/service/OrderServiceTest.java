package com.apda.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.apda.order.entity.Order;
import com.apda.order.entity.OrderEventOutbox;
import com.apda.order.event.OrderEventEnvelopeFactory;
import com.apda.order.repository.OrderEventOutboxRepository;
import com.apda.order.repository.OrderRepository;

class OrderServiceTest {

	private static final String AUTHORIZATION = "Bearer test-token";
	private static final String PRODUCT_URL = "http://produk.local:8083";
	private static final String CUSTOMER_URL = "http://pelanggan.local:8082";

	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final OrderEventOutboxRepository outboxRepository = mock(OrderEventOutboxRepository.class);
	private final OrderEventEnvelopeFactory envelopeFactory = new OrderEventEnvelopeFactory();
	private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

	private OrderService orderService;

	@BeforeEach
	void setUp() {
		ServiceInstance productInstance = mock(ServiceInstance.class);
		when(productInstance.getUri()).thenReturn(URI.create(PRODUCT_URL));
		when(discoveryClient.getInstances("produk")).thenReturn(List.of(productInstance));

		ServiceInstance customerInstance = mock(ServiceInstance.class);
		when(customerInstance.getUri()).thenReturn(URI.create(CUSTOMER_URL));
		when(discoveryClient.getInstances("pelanggan")).thenReturn(List.of(customerInstance));

		orderService = new OrderService(orderRepository, outboxRepository, envelopeFactory,
				discoveryClient, restTemplate);
	}

	@Test
	void forwardsAuthorizationAndReturnsOrderDetails() {
		Order order = new Order();
		order.setProdukId(1L);
		order.setPelangganId(2L);
		order.setTglTrans(LocalDate.of(2026, 10, 29));
		order.setJumlah(1);
		order.setTotal(new BigDecimal("12000000.00"));
		when(orderRepository.findAll()).thenReturn(List.of(order));

		server.expect(requestTo(PRODUCT_URL + "/api/produk/1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", AUTHORIZATION))
				.andRespond(withSuccess(
						"{\"id\":1,\"nama\":\"Laptop\",\"harga\":12000000,\"deskripsi\":\"Laptop\"}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(CUSTOMER_URL + "/api/pelanggan/2"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", AUTHORIZATION))
				.andRespond(withSuccess(
						"{\"id\":2,\"nama\":\"Amel\",\"alamat\":\"Padang\",\"jenis_kelamin\":\"P\"}",
						MediaType.APPLICATION_JSON));

		var details = orderService.findAllWithDetails(AUTHORIZATION, "admin", true);

		assertEquals(1, details.size());
		assertEquals("Laptop", details.get(0).getProduk().getNama());
		assertEquals("Amel", details.get(0).getPelanggan().getNama());
		server.verify();
	}

	@Test
	void propagatesDownstreamAuthorizationFailureInsteadOfDroppingOrder() {
		Order order = new Order();
		order.setProdukId(1L);
		order.setPelangganId(2L);
		when(orderRepository.findAll()).thenReturn(List.of(order));

		server.expect(requestTo(PRODUCT_URL + "/api/produk/1"))
				.andRespond(withSuccess(
						"{\"id\":1,\"nama\":\"Laptop\",\"harga\":12000000,\"deskripsi\":\"Laptop\"}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(CUSTOMER_URL + "/api/pelanggan/2"))
				.andExpect(header("Authorization", AUTHORIZATION))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThrows(HttpClientErrorException.class,
				() -> orderService.findAllWithDetails(AUTHORIZATION, "admin", true));
		server.verify();
	}

	@Test
	void persistsOutboxEventForSnapshotBeforeDispatch() {
		Order order = new Order();
		order.setProdukId(1L);
		order.setPelangganId(2L);
		order.setTglTrans(LocalDate.of(2026, 10, 30));
		order.setJumlah(2);
		order.setTotal(new BigDecimal("24000000.00"));
		when(orderRepository.save(order)).thenAnswer(invocation -> {
			Order argument = invocation.getArgument(0);
			argument.setId(7L);
			return argument;
		});

		server.expect(requestTo(PRODUCT_URL + "/api/produk/1"))
				.andRespond(withSuccess(
						"{\"id\":1,\"nama\":\"Laptop\",\"harga\":12000000,\"deskripsi\":\"Laptop\"}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(CUSTOMER_URL + "/api/pelanggan/2"))
				.andRespond(withSuccess(
						"{\"id\":2,\"nama\":\"Amel\",\"alamat\":\"Padang\",\"jenis_kelamin\":\"P\"}",
						MediaType.APPLICATION_JSON));

		Order saved = orderService.save(order, AUTHORIZATION, "admin");

		assertEquals(1L, saved.getAggregateVersion());
		assertEquals("admin", saved.getOwnerSubject());

		ArgumentCaptor<OrderEventOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderEventOutbox.class);
		verify(outboxRepository).save(outboxCaptor.capture());
		OrderEventOutbox pendingEvent = outboxCaptor.getValue();
		assertEquals(7L, pendingEvent.getAggregateId());
		assertEquals(1L, pendingEvent.getAggregateVersion());
		assertEquals("ORDER_CREATED", pendingEvent.getEventType());
		assertTrue(pendingEvent.getPayload().contains("\"eventType\":\"ORDER_CREATED\""));
		assertTrue(pendingEvent.getPayload().contains("\"eventId\":\"" + pendingEvent.getEventId() + "\""));
		assertTrue(pendingEvent.getPayload().contains("\"ownerSubject\":\"admin\""));
		assertTrue(pendingEvent.getPayload().contains("\"nama\":\"Laptop\""));
		assertEquals(7L, saved.getId());
		server.verify();
	}

	@Test
	void doesNotPersistOrderWhenReferencedProductDoesNotExist() {
		Order order = new Order();
		order.setProdukId(404L);
		order.setPelangganId(2L);
		order.setTglTrans(LocalDate.of(2026, 10, 30));
		order.setJumlah(1);
		order.setTotal(new BigDecimal("100.00"));

		server.expect(requestTo(PRODUCT_URL + "/api/produk/404"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> orderService.save(order, AUTHORIZATION, "siswa1"));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
		verify(outboxRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
		server.verify();
	}

	@Test
	void updateForwardsJwtForCustomerSnapshotAndUsesOutboxEventIdInPayload() {
		Order existingOrder = new Order();
		existingOrder.setId(8L);
		existingOrder.setProdukId(1L);
		existingOrder.setPelangganId(2L);
		existingOrder.setTglTrans(LocalDate.of(2026, 10, 29));
		existingOrder.setJumlah(1);
		existingOrder.setTotal(new BigDecimal("12000000.00"));
		existingOrder.setAggregateVersion(1L);
		existingOrder.setOwnerSubject("siswa1");
		when(orderRepository.findByIdAndOwnerSubject(8L, "siswa1")).thenReturn(Optional.of(existingOrder));
		when(orderRepository.save(existingOrder)).thenReturn(existingOrder);

		Order requestedOrder = new Order();
		requestedOrder.setProdukId(1L);
		requestedOrder.setPelangganId(2L);
		requestedOrder.setTglTrans(LocalDate.of(2026, 10, 30));
		requestedOrder.setJumlah(2);
		requestedOrder.setTotal(new BigDecimal("24000000.00"));

		server.expect(requestTo(PRODUCT_URL + "/api/produk/1"))
				.andExpect(header("Authorization", AUTHORIZATION))
				.andRespond(withSuccess(
						"{\"id\":1,\"nama\":\"Laptop\",\"harga\":12000000,\"deskripsi\":\"Laptop\"}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(CUSTOMER_URL + "/api/pelanggan/2"))
				.andExpect(header("Authorization", AUTHORIZATION))
				.andRespond(withSuccess(
						"{\"id\":2,\"nama\":\"Amel\",\"alamat\":\"Padang\",\"jenis_kelamin\":\"P\"}",
						MediaType.APPLICATION_JSON));

		Order updated = orderService.update(8L, requestedOrder, AUTHORIZATION, "siswa1", false).orElseThrow();

		assertEquals(2L, updated.getAggregateVersion());
		ArgumentCaptor<OrderEventOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderEventOutbox.class);
		verify(outboxRepository).save(outboxCaptor.capture());
		OrderEventOutbox event = outboxCaptor.getValue();
		assertTrue(event.getPayload().contains("\"eventId\":\"" + event.getEventId() + "\""));
		assertTrue(event.getPayload().contains("\"nama\":\"Amel\""));
		server.verify();
	}

	@Test
	void userCannotUpdateAnOrderOwnedByAnotherUser() {
		when(orderRepository.findByIdAndOwnerSubject(9L, "siswa1")).thenReturn(Optional.empty());

		Optional<Order> updated = orderService.update(9L, new Order(), AUTHORIZATION, "siswa1", false);

		assertTrue(updated.isEmpty());
		verify(outboxRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
	}
}
