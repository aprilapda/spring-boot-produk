package com.apda.orderquery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.apda.orderquery.entity.ProcessedOrderEvent;

@Repository
public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, String> {
}
