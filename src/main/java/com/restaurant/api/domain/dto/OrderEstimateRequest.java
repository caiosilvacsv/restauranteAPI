package com.restaurant.api.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderEstimateRequest(
    @NotNull(message = "O tempo estimado em minutos é obrigatório")
    @Positive(message = "O tempo estimado deve ser maior que zero")
    Integer estimatedMinutes
) {
}
