package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Ingredient;

import java.math.BigDecimal;

public record IngredientResponse(
    Long id,
    String name,
    BigDecimal price,
    Boolean active
) {
  public static IngredientResponse fromEntity(Ingredient ingredient) {
    return new IngredientResponse(
        ingredient.getId(),
        ingredient.getName(),
        ingredient.getPrice(),
        ingredient.getActive()
    );
  }
}
