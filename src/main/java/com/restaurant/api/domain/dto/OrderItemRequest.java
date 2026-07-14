package com.restaurant.api.domain.dto;

import java.util.Set;

public record OrderItemRequest(
    Long productId,
    Integer quantity,
    String note,
    Set<Long> addIngredients,
    Set<Long> removeIngredients
) {
}
