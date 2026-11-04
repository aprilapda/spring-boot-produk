package com.apda.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.apda.order.entity.Order;
import com.apda.order.entity.OrderEventOutbox;
import com.apda.order.event.OrderEventConstants;
import com.apda.order.event.OrderEventEnvelopeFactory;
import com.apda.order.repository.OrderEventOutboxRepository;
import com.apda.order.repository.OrderRepository;
import com.apda.order.vo.Pelanggan;
import com.apda.order.vo.Produk;
import com.apda.order.vo.ResponseTemplate;

import java.time.Instant;
import java.math.BigDecimal;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderEventOutboxRepository outboxRepository;
	private final OrderEventEnvelopeFactory envelopeFactory;
	private final DiscoveryClient discoveryClient;
	private final RestTemplate restTemplate;

	public OrderService(OrderRepository orderRepository, OrderEventOutboxRepository outboxRepository,
			OrderEventEnvelopeFactory envelopeFactory, DiscoveryClient discoveryClient, RestTemplate restTemplate) {
		this.orderRepository = orderRepository;
		this.outboxRepository = outboxRepository;
		this.envelopeFactory = envelopeFactory;
		this.discoveryClient = discoveryClient;
		this.restTemplate = restTemplate;
	}

	public List<ResponseTemplate> findAllWithDetails(String authorization, String ownerSubject, boolean administrator) {
		List<Order> orders = administrator
				? orderRepository.findAll()
				: ownerSubject == null ? List.of() : orderRepository.findAllByOwnerSubjectOrderByIdAsc(ownerSubject);
		return orders.stream()
				.map(order -> createResponseTemplate(order, authorization))
				.toList();
	}

	public Optional<ResponseTemplate> findWithDetailsById(Long id, String authorization, String ownerSubject,
			boolean administrator) {
		return findAccessibleById(id, ownerSubject, administrator)
				.map(order -> createResponseTemplate(order, authorization));
	}

	// Fallback nama aman tanpa mengubah perilaku sistem yang sudah berjalan.
	private ResponseTemplate createResponseTemplate(Order order, String authorization) {
		Produk produk = getFromService("produk", "/api/produk/" + order.getProdukId(), Produk.class, authorization);
		Pelanggan pelanggan = getFromService("pelanggan", "/api/pelanggan/" + order.getPelangganId(),
				Pelanggan.class, authorization);
		return new ResponseTemplate(order, produk, pelanggan);
	}

	private <T> T getFromService(String serviceName, String path, Class<T> responseType, String authorization) {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
		if (instances.isEmpty()) {
			throw new IllegalStateException("Service " + serviceName + " tidak tersedia");
		}

		HttpHeaders headers = new HttpHeaders();
		if (authorization != null && !authorization.isBlank()) {
			headers.set(HttpHeaders.AUTHORIZATION, authorization);
		}

		ResponseEntity<T> response = restTemplate.exchange(
				instances.get(0).getUri() + path,
				HttpMethod.GET,
				new HttpEntity<>(headers),
				responseType);
		return response.getBody();
	}

	// CREATE: simpan order + outbox event pada satu transaksi, lalu dispatcher
	// kirim ke RabbitMQ. Email tidak dikirim di sini lagi (alur baru via
	// abl.email.order-created di Consumer, dipicu event ORDER_CREATED).
	@Transactional
	public Order save(Order order, String authorization, String ownerSubject) {
		validateOrder(order);
		Produk produk = requireReference("produk", "/api/produk/" + order.getProdukId(), Produk.class,
				order.getProdukId(), authorization);
		Pelanggan pelanggan = requireReference("pelanggan", "/api/pelanggan/" + order.getPelangganId(),
				Pelanggan.class, order.getPelangganId(), authorization);
		long version = order.getAggregateVersion() + 1;
		order.setAggregateVersion(version);
		order.setOwnerSubject(ownerSubject);
		Order savedOrder = orderRepository.save(order);

		simpanEventOutbox(OrderEventConstants.EVENT_ORDER_CREATED, savedOrder, produk, pelanggan);
		return savedOrder;
	}

	// UPDATE: versi agregat naik satu, event snapshot lengkap via outbox.
	@Transactional
	public Optional<Order> update(Long id, Order order, String authorization, String ownerSubject,
			boolean administrator) {
		return findAccessibleById(id, ownerSubject, administrator).map(existingOrder -> {
			validateOrder(order);
			Produk produk = requireReference("produk", "/api/produk/" + order.getProdukId(), Produk.class,
					order.getProdukId(), authorization);
			Pelanggan pelanggan = requireReference("pelanggan", "/api/pelanggan/" + order.getPelangganId(),
					Pelanggan.class, order.getPelangganId(), authorization);
			long version = existingOrder.getAggregateVersion() + 1;
			existingOrder.setProdukId(order.getProdukId());
			existingOrder.setPelangganId(order.getPelangganId());
			existingOrder.setTglTrans(order.getTglTrans());
			existingOrder.setJumlah(order.getJumlah());
			existingOrder.setTotal(order.getTotal());
			existingOrder.setAggregateVersion(version);
			Order updatedOrder = orderRepository.save(existingOrder);

			simpanEventOutbox(OrderEventConstants.EVENT_ORDER_UPDATED, updatedOrder, produk, pelanggan);
			return updatedOrder;
		});
	}

	// DELETE: order dihapus, tetapi event ORDER_DELETED meneruskan versi ter +
	// 1 sebagai "tombstone" agar event lama tidak bisa menghidupkan ulang data.
	@Transactional
	public Optional<Long> deleteById(Long id, String ownerSubject, boolean administrator) {
		return findAccessibleById(id, ownerSubject, administrator).map(existingOrder -> {
			long deletionVersion = existingOrder.getAggregateVersion() + 1;
			orderRepository.delete(existingOrder);

			String eventId = UUID.randomUUID().toString();
			String payload = envelopeFactory.serialize(eventId, OrderEventConstants.EVENT_ORDER_DELETED,
					id, deletionVersion, null, null, null, null);
			outboxRepository.save(new OrderEventOutbox(eventId,
					OrderEventConstants.EVENT_ORDER_DELETED, id, deletionVersion, Instant.now(), payload));
			return deletionVersion;
		});
	}

	private Optional<Order> findAccessibleById(Long id, String ownerSubject, boolean administrator) {
		if (administrator) {
			return orderRepository.findById(id);
		}
		if (ownerSubject == null || ownerSubject.isBlank()) {
			return Optional.empty();
		}
		return orderRepository.findByIdAndOwnerSubject(id, ownerSubject);
	}

	// Snapshot event-time produk & pelanggan dibaca dari service pemilik data
	// selama transaksi command, sebelum envelope dan baris outbox disimpan.
	// Gagal memanggil service tidak menggagalkan tulisan order (toleran):
	// snapshot boleh null dan dikurasi lewat log.
	private void simpanEventOutbox(String eventType, Order order, Produk produk, Pelanggan pelanggan) {
		String eventId = UUID.randomUUID().toString();
		String payload = envelopeFactory.serialize(eventId, eventType, order.getId(), order.getAggregateVersion(),
				order.getOwnerSubject(), order, produk, pelanggan);
		outboxRepository.save(new OrderEventOutbox(eventId, eventType,
				order.getId(), order.getAggregateVersion(), Instant.now(), payload));
	}

	private void validateOrder(Order order) {
		if (order == null || order.getProdukId() == null || order.getProdukId() <= 0
				|| order.getPelangganId() == null || order.getPelangganId() <= 0
				|| order.getTglTrans() == null || order.getJumlah() == null || order.getJumlah() <= 0
				|| order.getTotal() == null || order.getTotal().compareTo(BigDecimal.ZERO) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"produkId, pelangganId, tglTrans, jumlah positif, dan total non-negatif wajib valid");
		}
	}

	private <T> T requireReference(String serviceName, String path, Class<T> responseType,
			Long expectedId, String authorization) {
		try {
			T body = getFromService(serviceName, path, responseType, authorization);
			if (body == null) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Service " + serviceName + " mengembalikan body kosong");
			}
			Long actualId = body instanceof Produk produk ? (long) produk.getId()
					: body instanceof Pelanggan pelanggan ? pelanggan.getId() : null;
			if (!expectedId.equals(actualId)) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Service " + serviceName + " mengembalikan ID referensi yang tidak cocok");
			}
			return body;
		} catch (HttpClientErrorException.NotFound exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					serviceName + " dengan ID " + expectedId + " tidak ditemukan atau bukan milik akun ini",
					exception);
		} catch (HttpClientErrorException exception) {
			if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
				throw new ResponseStatusException(exception.getStatusCode(),
						"Service " + serviceName + " menolak otorisasi", exception);
			}
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
					"Service " + serviceName + " gagal memvalidasi referensi", exception);
		} catch (IllegalStateException | RestClientException exception) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Service " + serviceName + " tidak tersedia untuk validasi referensi", exception);
		} catch (RuntimeException exception) {
			if (exception instanceof ResponseStatusException responseStatusException) {
				throw responseStatusException;
			}
			throw exception;
		}
	}
}
