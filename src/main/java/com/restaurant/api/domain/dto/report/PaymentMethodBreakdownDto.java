package com.restaurant.api.domain.dto.report;

import com.restaurant.api.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodBreakdownDto(
    PaymentMethod method,
    String description,
    BigDecimal totalAmount,
    long count,
    BigDecimal percentage
) {}
