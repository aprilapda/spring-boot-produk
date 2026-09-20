package com.apda.order.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.apda.order.entity.Order;
import com.apda.order.repository.OrderRepository;

@Service
public class OrderService {

	private final OrderRepository orderRepository;

	public OrderService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	public List<Order> findAll() {
		return orderRepository.findAll();
	}

	public Optional<Order> findById(Long id) {
		return orderRepository.findById(id);
	}

	public Order save(Order order) {
		return orderRepository.save(order);
	}

	public Optional<Order> update(Long id, Order order) {
		return orderRepository.findById(id).map(existingOrder -> {
			existingOrder.setProdukId(order.getProdukId());
			existingOrder.setPelangganId(order.getPelangganId());
			existingOrder.setTglTrans(order.getTglTrans());
			existingOrder.setJumlah(order.getJumlah());
			existingOrder.setTotal(order.getTotal());
			return orderRepository.save(existingOrder);
		});
	}

	public boolean deleteById(Long id) {
		if (!orderRepository.existsById(id)) {
			return false;
		}

		orderRepository.deleteById(id);
		return true;
	}
}