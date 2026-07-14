package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.ProductRequest;
import com.restaurant.api.domain.dto.ProductResponse;
import com.restaurant.api.domain.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

  private final ProductService service;
  public ProductController(ProductService service) {this.service = service;}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponse register(@RequestBody @Valid ProductRequest request) {
    return service.register(request);
  }

  @GetMapping
  public Page<ProductResponse> findById(Pageable pageable) {
    return service.findAll(pageable);
  }

  @GetMapping("/{id}")
  public ProductResponse findById(@PathVariable Long id) {
    return service.findById(id);
  }

  @PutMapping("/{id}")
  public ProductResponse update(@PathVariable Long id, @Valid ProductRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

}
