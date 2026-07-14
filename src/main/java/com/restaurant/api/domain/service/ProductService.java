package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.ProductRequest;
import com.restaurant.api.domain.dto.ProductResponse;
import com.restaurant.api.domain.entity.CategoryProduct;
import com.restaurant.api.domain.entity.Product;
import com.restaurant.api.domain.entity.Ingredient;
import com.restaurant.api.domain.repository.CategoryProductRepository;
import com.restaurant.api.domain.repository.ProductRepository;
import com.restaurant.api.domain.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private CategoryProductRepository categoryProductRepository;
  @Autowired
  private IngredientRepository ingredientRepository;

  public ProductResponse register(ProductRequest request) {
    CategoryProduct categoryProduct = findCategoryById(request.categoryId());
    Product product = request.toEntity(categoryProduct);

    if (request.ingredientsId() != null) {
      List<Ingredient> ingredients = ingredientRepository.findAllById(request.ingredientsId());
      product.getIngredient().clear();
      product.getIngredient().addAll(ingredients);
    }

    Product savedProduct = productRepository.save(product);
    return ProductResponse.fromEntity(savedProduct);
  }

  public Page<ProductResponse> findAll(Pageable pageable) {
    return productRepository.findByActiveTrue(pageable).map(ProductResponse::fromEntity);
  }

  public ProductResponse findById(Long id) {
    Product product = findProductById(id);
    return ProductResponse.fromEntity(product);
  }

  public ProductResponse update(Long id, ProductRequest request) {
    Product product = findProductById(id);
    CategoryProduct categoryProduct = findCategoryById(request.categoryId());

    request.toPopulate(product, categoryProduct);

    if (request.ingredientsId() != null) {
      List<Ingredient> ingredients = ingredientRepository.findAllById(request.ingredientsId());
      product.getIngredient().clear();
      product.getIngredient().addAll(ingredients);
    } else {
      product.getIngredient().clear();
    }

    Product updatedProduct = productRepository.save(product);
    return ProductResponse.fromEntity(updatedProduct);
  }

  public void delete(Long id) {
    Product product = findProductById(id);
    product.setActive(false);
    productRepository.save(product);
  }

  public Page<ProductResponse> findProductDelete(Pageable pageable) {
    return productRepository.findByActiveFalse(pageable).map(ProductResponse::fromEntity);
  }

  public Product findProductById(Long id) {
    return productRepository.findById(id).orElseThrow(
        () -> new RuntimeException("Product not found"));
  }

  public CategoryProduct findCategoryById(Long id) {
    return categoryProductRepository.findById(id).orElseThrow(
        () -> new RuntimeException("Category not found"));
  }

}
