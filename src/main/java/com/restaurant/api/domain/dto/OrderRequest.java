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
    List<OrderItemRequest> items,
    String orderCode,
    List<OrderPaymentRequest> payments
) {
  // Construtor de compatibilidade para código que não envia payments
  public OrderRequest(
      Long tableId,
      OrderType orderType,
      Long addressId,
      DeliveryAddressDto deliveryAddress,
      Boolean saveAddress,
      PaymentMethod paymentMethod,
      BigDecimal cashChangeFor,
      String note,
      List<OrderItemRequest> items,
      String orderCode
  ) {
    this(tableId, orderType, addressId, deliveryAddress, saveAddress, paymentMethod, cashChangeFor, note, items, orderCode, null);
  }

  // Construtor de compatibilidade para código que não envia orderCode nem payments
  public OrderRequest(
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
    this(tableId, orderType, addressId, deliveryAddress, saveAddress, paymentMethod, cashChangeFor, note, items, null, null);
  }

  // Alias de compatibilidade com versões anteriores
  public Long id() {
    return this.tableId;
  }
}
