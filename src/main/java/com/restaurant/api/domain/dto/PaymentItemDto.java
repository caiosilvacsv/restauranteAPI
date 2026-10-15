package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Payment;
import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusPayment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentItemDto(
    Long id,
    BigDecimal amount,
    PaymentMethod method,
    StatusPayment status,
    String payerName,
    Instant paymentDate
) {
  public static PaymentItemDto fromEntity(Payment payment) {
    return new PaymentItemDto(
        payment.getId(),
        payment.getAmount(),
        payment.getMethod(),
        payment.getStatus(),
        payment.getPayerName(),
        payment.getPaymentDate() != null ? payment.getPaymentDate() : payment.getCreateAt()
    );
  }
}
