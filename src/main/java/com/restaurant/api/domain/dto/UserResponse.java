package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.infra.security.TokenService;


public record UserResponse (
    Long id,
    String login,
    String token,
    String name,
    String role,
    String number,
    Boolean active
){

  public static UserResponse fromEntity (
      User request,
      String token
  ) {
    return new UserResponse(
        request.getId(),
        request.getLogin(),
        token,
        request.getName(),
        request.getRole() != null ? request.getRole().toString() : null,
        request.getNumber(),
        request.getActive()
    );
  }

  public static UserResponse fromEntity (User request) {
    return fromEntity(request, null);
  }
}
