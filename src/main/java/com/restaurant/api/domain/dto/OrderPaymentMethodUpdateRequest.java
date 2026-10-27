package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderPaymentMethodUpdateRequest(
    @NotNull(message = "O meio de pagamento é obrigatório")
    PaymentMethod paymentMethod,

    @Positive(message = "O valor para troco deve ser positivo")
    BigDecimal cashChangeFor
) {}
