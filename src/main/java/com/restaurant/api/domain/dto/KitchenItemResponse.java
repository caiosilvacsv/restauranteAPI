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
    Integer tableNumber,
    String location,
    String productName,
    String secondProductName,
    Long sizeId,
    String sizeName,
    String sizeShortName,
    Integer slices,
    Boolean isHalfAndHalf,
    Integer quantity,
    String note,
    String notesFirstHalf,
    String notesSecondHalf,
    BigDecimal unitPrice,
    StatusOrderItem status,
    List<KitchenModificationResponse> modifications
) {

  public static KitchenItemResponse fromEntity(OrderItem item) {

    List<KitchenModificationResponse> modificationsSorted = item.getModifications() != null
        ? item.getModifications()
            .stream()
            .map(KitchenModificationResponse :: fromEntity)
            .sorted(
                Comparator.comparing(m -> m.type()
                    .equals(ModificationType.REMOVE) ? 0 : 1))
            .toList()
        : List.of();

    Integer tableNumber = (item.getOrder() != null && item.getOrder().getTable() != null)
        ? item.getOrder().getTable().getNumber()
        : null;

    String location = tableNumber != null
        ? "Mesa " + tableNumber
        : (item.getOrder() != null ? item.getOrder().getOrderType() + " #" + item.getOrder().getId() : "Balcão");

    String secondProductName = item.getSecondProduct() != null ? item.getSecondProduct().getName() : null;
    Long sizeId = item.getPizzaSize() != null ? item.getPizzaSize().getId() : null;
    String sizeName = item.getPizzaSize() != null ? item.getPizzaSize().getName() : null;
    String sizeShortName = item.getPizzaSize() != null ? item.getPizzaSize().getShortName() : null;
    Integer slices = item.getPizzaSize() != null ? item.getPizzaSize().getSlices() : null;

    return new KitchenItemResponse(
        item.getId(),
        item.getOrder() != null ? item.getOrder().getId() : null,
        tableNumber,
        location,
        item.getProduct() != null ? item.getProduct().getName() : null,
        secondProductName,
        sizeId,
        sizeName,
        sizeShortName,
        slices,
        item.isHalfAndHalf(),
        item.getQuantity(),
        item.getNotes(),
        item.getNotesFirstHalf(),
        item.getNotesSecondHalf(),
        item.getUnitPrice(),
        item.getStatus(),
        modificationsSorted
    );
  }
}
