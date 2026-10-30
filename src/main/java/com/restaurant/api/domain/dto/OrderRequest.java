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
    List<OrderPaymentRequest> payments,
    String clientOrderId
) {
  // Cria uma nova instância de OrderRequest atribuindo o clientOrderId
  public OrderRequest withClientOrderId(String newClientOrderId) {
    return new OrderRequest(
        this.tableId, this.orderType, this.addressId, this.deliveryAddress,
        this.saveAddress, this.paymentMethod, this.cashChangeFor, this.note,
        this.items, this.orderCode, this.payments, newClientOrderId
    );
  }

  // Construtor de compatibilidade para código que envia até payments
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
      String orderCode,
      List<OrderPaymentRequest> payments
  ) {
    this(tableId, orderType, addressId, deliveryAddress, saveAddress, paymentMethod, cashChangeFor, note, items, orderCode, payments, null);
  }

  // Construtor de compatibilidade para código que não envia payments nem clientOrderId
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
    this(tableId, orderType, addressId, deliveryAddress, saveAddress, paymentMethod, cashChangeFor, note, items, orderCode, null, null);
  }

  // Construtor de compatibilidade para código que não envia orderCode, payments nem clientOrderId
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
    this(tableId, orderType, addressId, deliveryAddress, saveAddress, paymentMethod, cashChangeFor, note, items, null, null, null);
  }

  // Alias de compatibilidade com versões anteriores
  public Long id() {
    return this.tableId;
  }
}
