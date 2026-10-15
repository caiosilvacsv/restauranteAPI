package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.CategoryRequest;
import com.restaurant.api.domain.dto.CategoryResponse;
import com.restaurant.api.domain.entity.CategoryProduct;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.CategoryProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

  private final CategoryProductRepository repository;

  public CategoryService(CategoryProductRepository repository) {
    this.repository = repository;
  }

  @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    if (repository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("Categoria já existe", "Já existe uma categoria cadastrada com o nome: " + request.name());
    }

    CategoryProduct category = new CategoryProduct();
    category.setName(request.name().trim());
    category.setActive(request.active() != null ? request.active() : true);

    return CategoryResponse.fromEntity(repository.save(category));
  }

  @org.springframework.cache.annotation.Cacheable(value = "categories", key = "'active'")
  public List<CategoryResponse> findAllActive() {
    return repository.findByActiveTrue()
        .stream()
        .map(CategoryResponse::fromEntity)
        .toList();
  }

  public Page<CategoryResponse> findAll(Pageable pageable) {
    return repository.findAll(pageable).map(CategoryResponse::fromEntity);
  }

  public CategoryResponse findById(Long id) {
    CategoryProduct category = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria não encontrada", "Não foi possível encontrar a categoria com ID: " + id));
    return CategoryResponse.fromEntity(category);
  }

  @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
  @Transactional
  public CategoryResponse update(Long id, CategoryRequest request) {
    CategoryProduct category = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria não encontrada", "Não foi possível encontrar a categoria com ID: " + id));

    if (request.name() != null && !request.name().isBlank()) {
      category.setName(request.name().trim());
    }
    if (request.active() != null) {
      category.setActive(request.active());
    }

    return CategoryResponse.fromEntity(repository.save(category));
  }

  @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
  @Transactional
  public void delete(Long id) {
    CategoryProduct category = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria não encontrada", "Não foi possível encontrar a categoria com ID: " + id));
    category.setActive(false);
    repository.save(category);
  }
}
