package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Invoice;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
    Long id,
    Long orderId,
    Integer numberTable,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal serviceFee,
    BigDecimal discount,
    BigDecimal total,
    Instant closedAt
) {
  public static InvoiceResponse fromEntity (Invoice invoice) {
    Integer numberTable = (invoice.getOrder() != null && invoice.getOrder().getTable() != null)
        ? invoice.getOrder().getTable().getNumber()
        : null;

    Long orderId = invoice.getOrder() != null ? invoice.getOrder().getId() : null;

    return new InvoiceResponse(
        invoice.getId(),
        orderId,
        numberTable,
        invoice.getSubtotal(),
        invoice.getDeliveryFee() != null ? invoice.getDeliveryFee() : BigDecimal.ZERO,
        invoice.getServiceFee(),
        invoice.getDiscount(),
        invoice.getTotalAmount(),
        invoice.getClosedAt() != null ? invoice.getClosedAt() : Instant.now()
    );
  }
}
