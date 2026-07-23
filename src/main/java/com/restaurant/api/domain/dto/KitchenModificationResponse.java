package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.OrderItemModification;
import com.restaurant.api.domain.enums.ModificationType;

public record KitchenModificationResponse(
    String ingredients,
    ModificationType type,
    Integer quantity
) {
  public static KitchenModificationResponse fromEntity(OrderItemModification modification) {
    return new KitchenModificationResponse(
        modification.getIngredient().getName(),
        modification.getModificationType(),
        modification.getQuantity()
    );
  }
}
