package com.restaurant.api.domain.dto;

import java.util.Set;

public record OrderItemRequest(
    Long productId,
    Long secondProductId,
    Long sizeId,
    Integer quantity,
    String note,
    String notesFirstHalf,
    String notesSecondHalf,
    Set<Long> addIngredients,
    Set<Long> removeIngredients
) {
}
