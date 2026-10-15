package com.restaurant.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    String status,
    String transactionCode
) {
}