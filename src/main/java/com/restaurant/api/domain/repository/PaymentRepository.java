package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Payment;
import com.restaurant.api.domain.enums.StatusPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  List<Payment> findByStatusOrderByIdAsc(StatusPayment statusPayment);
  List<Payment> findByOrderId(Long orderId);
  List<Payment> findByOrderIdAndStatus(Long orderId, StatusPayment status);
  List<Payment> findByOrderIdInAndStatus(java.util.Collection<Long> orderIds, StatusPayment status);
  List<Payment> findByInvoiceId(Long invoiceId);
}
