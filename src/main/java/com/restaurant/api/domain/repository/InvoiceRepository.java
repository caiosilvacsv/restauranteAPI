package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  boolean existsByOrderId(Long orderId);

  Optional<Invoice> findByOrderId(Long orderId);
}
