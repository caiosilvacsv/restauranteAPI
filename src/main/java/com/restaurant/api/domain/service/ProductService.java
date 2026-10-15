package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.ProductRequest;
import com.restaurant.api.domain.dto.ProductResponse;
import com.restaurant.api.domain.entity.CategoryProduct;
import com.restaurant.api.domain.entity.Product;
import com.restaurant.api.domain.entity.Ingredient;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.CategoryProductRepository;
import com.restaurant.api.domain.repository.ProductRepository;
import com.restaurant.api.domain.repository.IngredientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final CategoryProductRepository categoryProductRepository;
  private final IngredientRepository ingredientRepository;
  private final com.restaurant.api.domain.repository.PizzaSizeRepository pizzaSizeRepository;

  public ProductService(
      ProductRepository productRepository,
      CategoryProductRepository categoryProductRepository,
      IngredientRepository ingredientRepository,
      com.restaurant.api.domain.repository.PizzaSizeRepository pizzaSizeRepository
  ) {
    this.productRepository = productRepository;
    this.categoryProductRepository = categoryProductRepository;
    this.ingredientRepository = ingredientRepository;
    this.pizzaSizeRepository = pizzaSizeRepository;
  }

  @org.springframework.cache.annotation.CacheEvict(value = "products", allEntries = true)
  public ProductResponse register(ProductRequest request) {
    CategoryProduct categoryProduct = findCategoryById(request.categoryId());
    Set<Ingredient> ingredients = findIngredientsByIds(request.ingredientsId());
    Product product = request.toEntity(categoryProduct, ingredients);

    syncSizePrices(product, request.sizes());

    Product savedProduct = productRepository.save(product);
    return ProductResponse.fromEntity(savedProduct);
  }

  @org.springframework.cache.annotation.Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
  public Page<ProductResponse> findAll(Pageable pageable) {
    return productRepository.findByActiveTrue(pageable).map(ProductResponse::fromEntity);
  }

  @org.springframework.cache.annotation.Cacheable(value = "products", key = "#id")
  public ProductResponse findById(Long id) {
    Product product = findProductById(id);
    return ProductResponse.fromEntity(product);
  }

  @org.springframework.cache.annotation.CacheEvict(value = "products", allEntries = true)
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = findProductById(id);
    CategoryProduct categoryProduct = findCategoryById(request.categoryId());
    Set<Ingredient> ingredients = findIngredientsByIds(request.ingredientsId());

    request.toPopulate(product, categoryProduct, ingredients);
    syncSizePrices(product, request.sizes());

    Product updatedProduct = productRepository.save(product);
    return ProductResponse.fromEntity(updatedProduct);
  }

  private void syncSizePrices(Product product, java.util.List<com.restaurant.api.domain.dto.ProductSizePriceDto> sizeDtos) {
    if (sizeDtos == null) {
      return;
    }
    product.getSizePrices().clear();
    for (var dto : sizeDtos) {
      com.restaurant.api.domain.entity.PizzaSize size = pizzaSizeRepository.findById(dto.sizeId())
          .orElseThrow(() -> new NotFoundException("Tamanho não encontrado", "ID do tamanho: " + dto.sizeId()));
      
      com.restaurant.api.domain.entity.ProductSizePrice psp = new com.restaurant.api.domain.entity.ProductSizePrice();
      psp.setProduct(product);
      psp.setPizzaSize(size);
      psp.setPrice(dto.price());
      product.getSizePrices().add(psp);
    }
  }

  @org.springframework.cache.annotation.CacheEvict(value = "products", allEntries = true)
  public void delete(Long id) {
    Product product = findProductById(id);
    product.setActive(false);
    productRepository.save(product);
  }

  public Page<ProductResponse> findProductDelete(Pageable pageable) {
    return productRepository
        .findByActiveFalse(pageable)
        .map(ProductResponse::fromEntity);
  }

  private Product findProductById(Long id) {
    return productRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Product not found"));
  }

  private CategoryProduct findCategoryById(Long id) {
    return categoryProductRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Category not found"));
  }

  private Set<Ingredient> findIngredientsByIds(Set<Long> ids) {
    if (ids == null || ids.isEmpty())
      return Collections.emptySet();
    return ingredientRepository.findAllByIdInAndActiveTrue(ids);
  }

}
