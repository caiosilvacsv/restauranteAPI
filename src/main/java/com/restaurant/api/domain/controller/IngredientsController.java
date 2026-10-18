package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.IngredientRequest;
import com.restaurant.api.domain.dto.IngredientResponse;
import com.restaurant.api.domain.service.IngredientsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/ingredient")
@Tag(name = "Ingredientes", description = "Endpoints para gerenciamento de ingredientes e adicionais")
public class IngredientsController {
  private final IngredientsService service;

  public IngredientsController(IngredientsService service) {
    this.service = service;
  }

  @PostMapping("/create")
  @Operation(summary = "Criar novo ingrediente (Admin)")
  public IngredientResponse createIngredient(@RequestBody IngredientRequest request) {
    return service.createIngredient(request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar ingrediente existente (Admin)")
  public IngredientResponse updateIngredient(@PathVariable Long id, @RequestBody IngredientRequest request) {
    return service.update(id, request);
  }

  @PutMapping("/{id}/delete")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Desativar ingrediente (Admin)")
  public void delete(@PathVariable Long id) {
    service.deleteById(id);
  }

  @PutMapping("/{id}/active")
  @Operation(summary = "Reativar ingrediente (Admin)")
  public IngredientResponse activeIngredient(@PathVariable Long id) {
    return service.activeIngredient(id);
  }

  @GetMapping()
  @Operation(summary = "Listar ingredientes ativos (paginado)")
  public Page<IngredientResponse> findAll(Pageable page) {
    return service.findAll(page);
  }

  @GetMapping("/delete")
  @Operation(summary = "Listar ingredientes desativados (Admin, paginado)")
  public Page<IngredientResponse> findAllDelete(Pageable page) {
    return service.findAllDelete(page);
  }

  @PostMapping("/create-multiple")
  @Operation(summary = "Criar múltiplos ingredientes em lote (Admin)")
  public List<IngredientResponse> saveAll(@RequestBody List<IngredientRequest> requests) {
    return service.saveAll(requests);
  }
}
