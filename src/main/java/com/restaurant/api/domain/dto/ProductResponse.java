package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Ingredient;
import com.restaurant.api.domain.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Boolean active,
    Integer preparationTime,
    Long categoryId,
    String categoryName,
    Instant createAt,
    Set<IngredientResponse> ingredientsId
) {
  public static ProductResponse fromEntity(Product product) {
    Set<IngredientResponse> ingredientsId = product.getIngredient()
        .stream()
        .map(IngredientResponse::fromEntity)
        .collect(Collectors.toSet());
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getActive(),
        product.getPreparationTimeInMinutes(),
        product.getCategoryProduct().getId(),
        product.getCategoryProduct().getName(),
        product.getCreateAt(),
        ingredientsId
    );
  }

  public record IngredientResponse(Long id, String name) {
    public static IngredientResponse fromEntity(Ingredient ingredient) {
      return new IngredientResponse(ingredient.getId(), ingredient.getName());
    }
  }
}
