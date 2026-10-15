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
    Long secondProductId,
    String nameSecondProduct,
    Long sizeId,
    String sizeName,
    String sizeShortName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal total,
    String notes,
    String notesFirstHalf,
    String notesSecondHalf,
    Boolean isHalfAndHalf,
    StatusOrderItem status,
    Set<OrderItemModificationResponse> modifications

) {
  public static OrderItemResponse fromEntity(OrderItem item ) {
    Set<OrderItemModificationResponse> modifications = item.getModifications() != null
        ? item.getModifications()
            .stream()
            .map(OrderItemModificationResponse :: fromEntity)
            .collect(Collectors.toSet())
        : Set.of();

    BigDecimal total = item.getUnitPrice() != null 
        ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        : BigDecimal.ZERO;

    Long secondProductId = item.getSecondProduct() != null ? item.getSecondProduct().getId() : null;
    String nameSecondProduct = item.getSecondProduct() != null ? item.getSecondProduct().getName() : null;

    Long sizeId = item.getPizzaSize() != null ? item.getPizzaSize().getId() : null;
    String sizeName = item.getPizzaSize() != null ? item.getPizzaSize().getName() : null;
    String sizeShortName = item.getPizzaSize() != null ? item.getPizzaSize().getShortName() : null;

    return new OrderItemResponse(
        item.getId(),
        item.getOrder() != null ? item.getOrder().getId() : null,
        item.getProduct() != null ? item.getProduct().getId() : null,
        item.getProduct() != null ? item.getProduct().getName() : null,
        secondProductId,
        nameSecondProduct,
        sizeId,
        sizeName,
        sizeShortName,
        item.getQuantity(),
        item.getUnitPrice(),
        total,
        item.getNotes(),
        item.getNotesFirstHalf(),
        item.getNotesSecondHalf(),
        item.isHalfAndHalf(),
        item.getStatus(),
        modifications
    );
  }
}
