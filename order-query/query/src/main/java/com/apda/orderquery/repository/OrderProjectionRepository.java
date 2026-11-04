package com.apda.orderquery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.apda.orderquery.entity.OrderProjection;

@Repository
public interface OrderProjectionRepository extends JpaRepository<OrderProjection, Long> {

    List<OrderProjection> findAllByOrderByOrderIdAsc();

    List<OrderProjection> findAllByOwnerSubjectOrderByOrderIdAsc(String ownerSubject);
}
