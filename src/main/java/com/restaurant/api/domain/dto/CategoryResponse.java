package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.CategoryProduct;

public record CategoryResponse(
    Long id,
    String name,
    Boolean active
) {
  public static CategoryResponse fromEntity(CategoryProduct category) {
    if (category == null) return null;
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getActive()
    );
  }
}
