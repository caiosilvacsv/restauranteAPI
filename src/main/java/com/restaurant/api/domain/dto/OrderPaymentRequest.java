package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderPaymentRequest(
    @NotNull(message = "O método de pagamento é obrigatório")
    PaymentMethod method,

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    BigDecimal amount,

    String payerName,

    BigDecimal cashGiven
) {
}
