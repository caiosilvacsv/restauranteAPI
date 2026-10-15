package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Ingredient;

import java.math.BigDecimal;

public record IngredientRequest(
    String name,
    BigDecimal price
) {
  public Ingredient toEntity() {
    Ingredient ingredient = new Ingredient();
    ingredient.setName(this.name);
    ingredient.setPrice(this.price != null ? this.price : BigDecimal.ZERO);
    ingredient.setActive(true);
    return ingredient;
  } 
}