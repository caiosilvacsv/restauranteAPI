package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.PizzaSizeRequest;
import com.restaurant.api.domain.dto.PizzaSizeResponse;
import com.restaurant.api.domain.service.PizzaSizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sizes")
@Tag(name = "Tamanhos de Pizza", description = "Endpoints para gerenciamento e consulta de tamanhos de pizzas (Broto, Média, Grande, Família)")
public class PizzaSizeController {

  private final PizzaSizeService pizzaSizeService;

  public PizzaSizeController(PizzaSizeService pizzaSizeService) {
    this.pizzaSizeService = pizzaSizeService;
  }

  @GetMapping
  @Operation(summary = "Listar tamanhos ativos", description = "Retorna todos os tamanhos de pizzas disponíveis no cardápio")
  public ResponseEntity<List<PizzaSizeResponse>> listActive() {
    return ResponseEntity.ok(pizzaSizeService.findAllActive());
  }

  @GetMapping("/all")
  @Operation(summary = "Listar todos os tamanhos (Admin)", description = "Retorna todos os tamanhos cadastrados (ativos e inativos)")
  public ResponseEntity<List<PizzaSizeResponse>> listAll() {
    return ResponseEntity.ok(pizzaSizeService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar tamanho por ID")
  public ResponseEntity<PizzaSizeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(pizzaSizeService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Cadastrar novo tamanho (Admin)")
  public ResponseEntity<PizzaSizeResponse> create(@Valid @RequestBody PizzaSizeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(pizzaSizeService.create(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar tamanho existente (Admin)")
  public ResponseEntity<PizzaSizeResponse> update(@PathVariable Long id, @Valid @RequestBody PizzaSizeRequest request) {
    return ResponseEntity.ok(pizzaSizeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Desativar tamanho (Admin)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    pizzaSizeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
