package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.enums.Role;

public record UserRegisterRequest (
    String login,
    String password,
    String name,
    String number
){
  public User toEntity(){
    User user = new User();
    user.setLogin(login);
    user.setPassword(password);
    user.setName(name);
    user.setNumber(number);
    user.setActive(true);
    user.setRole(Role.USER);
    return user;
  }
}
