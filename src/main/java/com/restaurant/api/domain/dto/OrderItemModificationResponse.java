package com.restaurant.api.domain.dto;
import com.restaurant.api.domain.entity.OrderItemModification;
import com.restaurant.api.domain.enums.ModificationType;

import java.math.BigDecimal;

public record OrderItemModificationResponse(
    Long id,
    String ingredientsName,
    ModificationType type,
    Integer quantity,
    BigDecimal extraPrice
) {
  public static OrderItemModificationResponse fromEntity(OrderItemModification modification ) {
    return new OrderItemModificationResponse(
        modification.getIngredient().getId(),
        modification.getIngredient().getName(),
        modification.getModificationType(),
        modification.getQuantity(),
        modification.getExtraPrice()
    );
  }
}
