package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.GuestRequest;
import com.restaurant.api.domain.dto.UserLoginRequest;
import com.restaurant.api.domain.dto.UserRegisterRequest;
import com.restaurant.api.domain.dto.UserResponse;
import com.restaurant.api.domain.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
  private final AuthService service;

  public AuthenticationController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponse> login (@RequestBody UserLoginRequest request){
    return ResponseEntity.ok(service.login(request));
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@RequestBody UserRegisterRequest request){
    return ResponseEntity.ok(service.register(request));
  }

  @PostMapping("/guest")
  public ResponseEntity<UserResponse> guest(@RequestBody GuestRequest request){
    return ResponseEntity.ok(service.loginGuest(request));
  }
}
