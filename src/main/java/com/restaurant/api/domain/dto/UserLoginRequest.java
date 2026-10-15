package com.restaurant.api.domain.dto;

public record UserLoginRequest(
    String login,
    String password
) {
}
