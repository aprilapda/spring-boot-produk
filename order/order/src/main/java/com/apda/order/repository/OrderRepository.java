package com.apda.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apda.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOwnerSubjectOrderByIdAsc(String ownerSubject);

    Optional<Order> findByIdAndOwnerSubject(Long id, String ownerSubject);
}
