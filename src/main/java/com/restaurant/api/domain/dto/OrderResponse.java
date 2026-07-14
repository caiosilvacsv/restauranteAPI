package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.enums.StatusOrder;

import java.time.Instant;

public record OrderResponse(
    Long id,
    Long tableId,
    Integer numberTable,
    Instant openedAt,
    Instant closedAt,
    StatusOrder status,
    String note
) {
  public static OrderResponse fromEntity(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getTablesID().getId(),
        order.getTablesID().getNumber(),
        order.getOpenedAt(),
        order.getClosedAt(),
        order.getStatus(),
        order.getNotes()
    );
  }
}
