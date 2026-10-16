package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffRegisterRequest(
    @NotBlank(message = "Login é obrigatório")
    String login,

    @NotBlank(message = "Senha é obrigatória")
    String password,

    @NotBlank(message = "Nome é obrigatório")
    String name,

    @NotBlank(message = "Número/Telefone é obrigatório")
    String number,

    @NotNull(message = "Cargo (Role) é obrigatório: ADMIN ou WAITER")
    Role role
) {}
