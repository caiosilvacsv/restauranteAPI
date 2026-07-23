package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Invoice;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
    Long id,
    Long orderId,
    Integer numberTable,
    BigDecimal subtotal,
    BigDecimal serviceFee,
    BigDecimal discount,
    BigDecimal total,
    Instant closedAt
) {
  public static InvoiceResponse fromEntity (Invoice invoice) {
    return new InvoiceResponse(
        invoice.getId(),
        invoice.getOrderID().getId(),
        invoice.getOrderID().getTablesID().getNumber(),
        invoice.getSubtotal(),
        invoice.getServiceFee(),
        invoice.getDiscount(),
        invoice.getTotalAmount(),
        Instant.now()
    );
  }
}
