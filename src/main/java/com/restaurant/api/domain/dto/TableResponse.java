package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.RestaurantTables;
import com.restaurant.api.domain.enums.StatusTable;

import java.time.Instant;

public record TableResponse(
    Long id,
    Integer number,
    String description,
    Integer capacity,
    StatusTable status,
    Long currentOrderId,
    String currentOrderCode,
    Instant openedAt
) {
  public static TableResponse fromEntity(RestaurantTables table, Long currentOrderId, String currentOrderCode, Instant openedAt) {
    return new TableResponse(
        table.getId(),
        table.getNumber(),
        table.getDescription(),
        table.getCapacity(),
        table.getStatus(),
        currentOrderId,
        currentOrderCode,
        openedAt
    );
  }
}
