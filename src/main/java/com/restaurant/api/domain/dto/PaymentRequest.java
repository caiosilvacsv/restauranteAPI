package com.restaurant.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurant.api.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
    BigDecimal amount,
    String paymentMethod
) {
}