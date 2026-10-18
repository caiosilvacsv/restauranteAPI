package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.CategoryRequest;
import com.restaurant.api.domain.dto.CategoryResponse;
import com.restaurant.api.domain.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias de Produtos", description = "Endpoints para gerenciamento de categorias do cardápio")
public class CategoryController {

  private final CategoryService service;

  public CategoryController(CategoryService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Listar categorias ativas")
  public ResponseEntity<List<CategoryResponse>> findAllActive() {
    return ResponseEntity.ok(service.findAllActive());
  }

  @GetMapping("/all")
  @Operation(summary = "Listar todas as categorias (Admin, paginado)")
  public ResponseEntity<Page<CategoryResponse>> findAll(Pageable pageable) {
    return ResponseEntity.ok(service.findAll(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar categoria por ID")
  public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PostMapping
  @Operation(summary = "Criar nova categoria (Admin)")
  public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar categoria existente (Admin)")
  public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Desativar categoria (Admin)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
