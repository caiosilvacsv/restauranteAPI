package com.restaurant.api.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderPaymentStatusResponse(
    Long orderId,
    String orderCode,
    BigDecimal totalAmount,
    BigDecimal totalPaid,
    BigDecimal remainingAmount,
    Boolean isFullyPaid,
    List<PaymentItemDto> payments
) {
}
