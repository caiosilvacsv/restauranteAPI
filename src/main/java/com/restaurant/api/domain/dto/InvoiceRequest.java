package com.restaurant.api.domain.dto;

import java.math.BigDecimal;

public record InvoiceRequest(
    BigDecimal serviceFee,
    BigDecimal discount
) {
}
