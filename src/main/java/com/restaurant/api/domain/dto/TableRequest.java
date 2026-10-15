package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.enums.StatusTable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TableRequest(
    @NotNull(message = "O número da mesa é obrigatório")
    @Positive(message = "O número da mesa deve ser maior que zero")
    Integer number,

    String description,

    @NotNull(message = "A capacidade da mesa é obrigatória")
    @Positive(message = "A capacidade deve ser maior que zero")
    Integer capacity,

    StatusTable status
) {
}
