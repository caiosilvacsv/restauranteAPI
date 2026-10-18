package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.ProductRequest;
import com.restaurant.api.domain.dto.ProductResponse;
import com.restaurant.api.domain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@Tag(name = "Produtos", description = "Endpoints para gerenciamento e consulta de produtos e pizzas do cardápio")
public class ProductController {

  private final ProductService service;
  public ProductController(ProductService service) {this.service = service;}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar novo produto (Admin)")
  public ProductResponse register(@RequestBody @Valid ProductRequest request) {
    return service.register(request);
  }

  @GetMapping
  @Operation(summary = "Listar produtos ativos (paginado)")
  public Page<ProductResponse> findAll(Pageable pageable) {
    return service.findAll(pageable);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar produto por ID")
  public ProductResponse findById(@PathVariable Long id) {
    return service.findById(id);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar produto existente (Admin)")
  public ProductResponse update(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Desativar produto (Admin)")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  @GetMapping("/delete")
  @Operation(summary = "Listar produtos desativados (Admin)")
  public Page<ProductResponse> disableProducts(Pageable pageable) {
    return service.findProductDelete(pageable);
  }
}
