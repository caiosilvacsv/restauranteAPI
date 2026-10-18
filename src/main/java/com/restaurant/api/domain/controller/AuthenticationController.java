package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.GuestRequest;
import com.restaurant.api.domain.dto.UserLoginRequest;
import com.restaurant.api.domain.dto.UserRegisterRequest;
import com.restaurant.api.domain.dto.UserResponse;
import com.restaurant.api.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
@Tag(name = "Autenticação", description = "Endpoints para login, registro de clientes e acesso como visitante (Guest)")
public class AuthenticationController {
  private final AuthService service;

  public AuthenticationController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/login")
  @Operation(summary = "Autenticar usuário (Admin, Garçom ou Cliente) e obter token JWT")
  public ResponseEntity<UserResponse> login (@RequestBody UserLoginRequest request){
    return ResponseEntity.ok(service.login(request));
  }

  @PostMapping("/register")
  @Operation(summary = "Cadastrar novo cliente (auto-registro)")
  public ResponseEntity<UserResponse> register(@RequestBody UserRegisterRequest request){
    return ResponseEntity.ok(service.register(request));
  }

  @PostMapping("/guest")
  @Operation(summary = "Login rápido como visitante/cliente temporário (por número de telefone)")
  public ResponseEntity<UserResponse> guest(@RequestBody GuestRequest request){
    return ResponseEntity.ok(service.loginGuest(request));
  }
}
