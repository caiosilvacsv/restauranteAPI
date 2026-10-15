package com.restaurant.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PizzaSizeRequest(
    @NotBlank(message = "O nome do tamanho é obrigatório")
    String name,

    @NotBlank(message = "O nome curto é obrigatório")
    @Size(max = 10, message = "O nome curto deve ter no máximo 10 caracteres")
    String shortName,

    @NotNull(message = "A quantidade de fatias é obrigatória")
    @Positive(message = "A quantidade de fatias deve ser maior que zero")
    Integer slices,

    @NotNull(message = "O número máximo de sabores é obrigatório")
    @Positive(message = "O número máximo de sabores deve ser de no mínimo 1")
    Integer maxFlavors,

    Boolean active
) {
}
