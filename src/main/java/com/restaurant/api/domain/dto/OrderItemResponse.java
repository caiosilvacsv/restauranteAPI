package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.StatusOrderItem;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public record OrderItemResponse(
    Long id,
    Long orderId,
    Long productId,
    String nameProduct,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal total,
    String notes,
    StatusOrderItem status,
    Set<OrderItemModificationResponse> modifications

) {
  public static OrderItemResponse fromEntity(OrderItem item ) {
    Set<OrderItemModificationResponse> modifications = item.getModifications()
        .stream()
        .map(OrderItemModificationResponse :: fromEntity)
        .collect(Collectors.toSet());

    BigDecimal total = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

    return new OrderItemResponse(
        item.getId(),
        item.getOrderID().getId(),
        item.getProductID().getId(),
        item.getProductID().getName(),
        item.getQuantity(),
        item.getUnitPrice(),
        total,
        item.getNotes(),
        item.getStatus(),
        modifications
    );
  }
}
