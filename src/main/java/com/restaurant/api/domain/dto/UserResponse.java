package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.infra.security.TokenService;


public record UserResponse (
    String login,
    String token,
    String name,
    String role,
    String number
){

  public static UserResponse fromEntity (
      User request,
      String token
  ) {

    return new  UserResponse(
        request.getLogin(),
        token,
        request.getName(),
        request.getRole().toString(),
        request.getNumber()
    );

  }
}
