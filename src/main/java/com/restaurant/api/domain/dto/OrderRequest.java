package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
    Long tableId,
    OrderType orderType,
    Long addressId,
    DeliveryAddressDto deliveryAddress,
    Boolean saveAddress,
    PaymentMethod paymentMethod,
    BigDecimal cashChangeFor,
    String note,
    List<OrderItemRequest> items
) {
  // Alias de compatibilidade com versões anteriores
  public Long id() {
    return this.tableId;
  }
}
