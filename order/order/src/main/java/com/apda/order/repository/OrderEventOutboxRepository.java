package com.apda.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.apda.order.entity.OrderEventOutbox;

@Repository
public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutbox, String> {

    List<OrderEventOutbox> findByDeliveredFalseOrderByOccurredAtAsc();
}
