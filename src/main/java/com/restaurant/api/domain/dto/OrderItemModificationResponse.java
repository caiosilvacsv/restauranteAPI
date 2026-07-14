package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.entity.OrderItemModification;

import java.math.BigDecimal;

public record OrderItemModificationResponse(
    Long id,
    String ingredientsName,
    String type,
    BigDecimal extraPrice
) {
  public static OrderItemModificationResponse fromEntity(OrderItemModification modification ) {
    return new OrderItemModificationResponse(
        modification.getIngredient().getId(),
        modification.getIngredient().getName(),
        modification.getModificationType().name(),
        modification.getExtraPrice()
    );
  }
}
