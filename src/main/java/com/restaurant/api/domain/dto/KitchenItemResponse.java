package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.ModificationType;
import com.restaurant.api.domain.enums.StatusOrderItem;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record KitchenItemResponse(
    Long itemId,
    Long orderId,
    Integer tableId,
    String productName,
    Integer quantity,
    String note,
    BigDecimal unitPrice,
    StatusOrderItem status,
    List<KitchenModificationResponse> modifications
) {

  public static KitchenItemResponse fromEntity(OrderItem item) {

    List<KitchenModificationResponse> modificationsSorted = item.getModifications()
        .stream()
        .map(KitchenModificationResponse :: fromEntity)
        .sorted(
            Comparator.comparing(m -> m.type()
                .equals(ModificationType.REMOVE) ? 0 : 1))
        .toList();

    return new KitchenItemResponse(
        item.getId(),
        item.getOrderID().getId(),
        item.getOrderID().getTablesID().getNumber(),
        item.getProductID().getName(),
        item.getQuantity(),
        item.getNotes(),
        item.getUnitPrice(),
        item.getStatus(),
        modificationsSorted
    );
  }
}
