package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.StaffRegisterRequest;
import com.restaurant.api.domain.dto.UserResponse;
import com.restaurant.api.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários e Equipe", description = "Gerenciamento de contas de administradores e garçons (restrito a ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/staff")
  @Operation(summary = "Cadastrar novo Administrador ou Garçom", description = "Permite a um ADMIN cadastrar novos membros da equipe (ADMIN ou WAITER)")
  public ResponseEntity<UserResponse> createStaff(@RequestBody @Valid StaffRegisterRequest request) {
    UserResponse response = userService.createStaff(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/staff")
  @Operation(summary = "Listar membros da equipe", description = "Retorna todos os usuários com papel ADMIN ou WAITER")
  public ResponseEntity<List<UserResponse>> listStaff() {
    return ResponseEntity.ok(userService.listStaff());
  }

  @PatchMapping("/{id}/toggle-active")
  @Operation(summary = "Ativar / Desativar usuário", description = "Alterna o status ativo de um usuário da equipe")
  public ResponseEntity<UserResponse> toggleActive(@PathVariable Long id) {
    return ResponseEntity.ok(userService.toggleActive(id));
  }
}
