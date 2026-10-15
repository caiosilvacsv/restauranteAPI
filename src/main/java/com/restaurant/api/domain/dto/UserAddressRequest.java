package com.restaurant.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAddressRequest(
    @Size(max = 50, message = "O apelido deve ter no máximo 50 caracteres")
    String nickname,

    @NotBlank(message = "A rua é obrigatória")
    @Size(max = 150, message = "A rua deve ter no máximo 150 caracteres")
    String street,

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
    String number,

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
    String neighborhood,

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
    String complement,

    String referencePoint,

    Boolean isDefault
) {
}
