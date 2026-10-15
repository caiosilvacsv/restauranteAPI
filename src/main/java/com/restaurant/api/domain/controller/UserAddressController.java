package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.UserAddressRequest;
import com.restaurant.api.domain.dto.UserAddressResponse;
import com.restaurant.api.domain.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@Tag(name = "Endereços do Usuário", description = "Endpoints para gerenciamento de endereços de entrega salvos na conta do cliente")
public class UserAddressController {

  private final UserAddressService userAddressService;

  public UserAddressController(UserAddressService userAddressService) {
    this.userAddressService = userAddressService;
  }

  @GetMapping
  @Operation(summary = "Listar endereços do usuário", description = "Retorna todos os endereços ativos do cliente logado, ordenados pelo padrão primeiro")
  public ResponseEntity<List<UserAddressResponse>> listMyAddresses() {
    return ResponseEntity.ok(userAddressService.findAllForCurrentUser());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar endereço por ID")
  public ResponseEntity<UserAddressResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(userAddressService.findByIdForCurrentUser(id));
  }

  @PostMapping
  @Operation(summary = "Cadastrar novo endereço")
  public ResponseEntity<UserAddressResponse> create(@Valid @RequestBody UserAddressRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userAddressService.create(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar endereço existente")
  public ResponseEntity<UserAddressResponse> update(@PathVariable Long id, @Valid @RequestBody UserAddressRequest request) {
    return ResponseEntity.ok(userAddressService.update(id, request));
  }

  @PatchMapping("/{id}/default")
  @Operation(summary = "Definir como endereço padrão")
  public ResponseEntity<UserAddressResponse> setDefault(@PathVariable Long id) {
    return ResponseEntity.ok(userAddressService.setDefault(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Remover endereço")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    userAddressService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
