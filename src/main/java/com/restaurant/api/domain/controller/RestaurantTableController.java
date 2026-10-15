package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.TableRequest;
import com.restaurant.api.domain.dto.TableResponse;
import com.restaurant.api.domain.enums.StatusTable;
import com.restaurant.api.domain.service.RestaurantTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tables")
@Tag(name = "Mesas do Restaurante", description = "Endpoints para consulta do mapa de mesas, status e identificação do pedido ativo em tempo real")
public class RestaurantTableController {

  private final RestaurantTableService tableService;

  public RestaurantTableController(RestaurantTableService tableService) {
    this.tableService = tableService;
  }

  @GetMapping
  @Operation(summary = "Listar todas as mesas", description = "Retorna todas as mesas com status e ID da comanda aberta caso esteja ocupada")
  public ResponseEntity<List<TableResponse>> listAll() {
    return ResponseEntity.ok(tableService.findAll());
  }

  @GetMapping("/available")
  @Operation(summary = "Listar mesas disponíveis")
  public ResponseEntity<List<TableResponse>> listAvailable() {
    return ResponseEntity.ok(tableService.findAvailable());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar mesa por ID")
  public ResponseEntity<TableResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(tableService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Cadastrar nova mesa (Admin)")
  public ResponseEntity<TableResponse> create(@Valid @RequestBody TableRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar mesa (Admin)")
  public ResponseEntity<TableResponse> update(@PathVariable Long id, @Valid @RequestBody TableRequest request) {
    return ResponseEntity.ok(tableService.update(id, request));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Alterar status da mesa (Admin e Waiter)")
  public ResponseEntity<TableResponse> updateStatus(@PathVariable Long id, @RequestParam StatusTable status) {
    return ResponseEntity.ok(tableService.updateStatus(id, status));
  }
}
