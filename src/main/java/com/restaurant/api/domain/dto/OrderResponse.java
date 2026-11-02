package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderCode,
    String clientOrderId,
    String customerName,
    String customerPhone,
    Long tableId,
    Integer numberTable,
    OrderType orderType,
    DeliveryAddressDto deliveryAddress,
    PaymentMethod paymentMethod,
    BigDecimal cashChangeFor,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal totalAmount,
    BigDecimal totalPaid,
    BigDecimal remainingAmount,
    Boolean isFullyPaid,
    Instant openedAt,
    Instant closedAt,
    StatusOrder status,
    String note,
    String cancelReason,
    Integer estimatedMinutes,
    Instant estimatedDeliveryAt,
    List<OrderItemResponse> items
) {
  // Construtor de compatibilidade para chamadas legadas de 24 argumentos sem clientOrderId
  public OrderResponse(
      Long id,
      String orderCode,
      String customerName,
      String customerPhone,
      Long tableId,
      Integer numberTable,
      OrderType orderType,
      DeliveryAddressDto deliveryAddress,
      PaymentMethod paymentMethod,
      BigDecimal cashChangeFor,
      BigDecimal subtotal,
      BigDecimal deliveryFee,
      BigDecimal totalAmount,
      BigDecimal totalPaid,
      BigDecimal remainingAmount,
      Boolean isFullyPaid,
      Instant openedAt,
      Instant closedAt,
      StatusOrder status,
      String note,
      String cancelReason,
      Integer estimatedMinutes,
      Instant estimatedDeliveryAt,
      List<OrderItemResponse> items
  ) {
    this(id, orderCode, null, customerName, customerPhone, tableId, numberTable, orderType, deliveryAddress, paymentMethod, cashChangeFor, subtotal, deliveryFee, totalAmount, totalPaid, remainingAmount, isFullyPaid, openedAt, closedAt, status, note, cancelReason, estimatedMinutes, estimatedDeliveryAt, items);
  }

  public static OrderResponse fromEntity(Order order) {
    return fromEntity(order, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
  }

  public static OrderResponse fromEntity(
      Order order,
      List<OrderItemResponse> items,
      BigDecimal subtotal,
      BigDecimal deliveryFee,
      BigDecimal totalAmount
  ) {
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal remaining = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    boolean isPaid = order.getStatus() == StatusOrder.CLOSED;
    return fromEntity(order, items, subtotal, deliveryFee, totalAmount, totalPaid, remaining, isPaid);
  }

  public static OrderResponse fromEntity(
      Order order,
      List<OrderItemResponse> items,
      BigDecimal subtotal,
      BigDecimal deliveryFee,
      BigDecimal totalAmount,
      BigDecimal totalPaid,
      BigDecimal remainingAmount,
      Boolean isFullyPaid
  ) {
    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    Integer numberTable = order.getTable() != null ? order.getTable().getNumber() : null;
    String orderCode = order.getOrderCode() != null 
        ? order.getOrderCode() 
        : (order.getId() != null ? String.format("#%04d", order.getId()) : null);

    String customerName = order.getCustomerName() != null && !order.getCustomerName().isBlank()
        ? order.getCustomerName()
        : (order.getUser() != null ? order.getUser().getName() : null);

    String customerPhone = order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()
        ? order.getCustomerPhone()
        : (order.getUser() != null ? order.getUser().getNumber() : null);

    BigDecimal effectiveSubtotal = subtotal;
    BigDecimal effectiveDeliveryFee = deliveryFee;
    BigDecimal effectiveTotalAmount = totalAmount;
    BigDecimal effectiveTotalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
    BigDecimal effectiveRemaining = remainingAmount != null ? remainingAmount : BigDecimal.ZERO;
    Boolean effectiveIsPaid = isFullyPaid != null ? isFullyPaid : false;

    if (order.getStatus() == StatusOrder.CANCELLED) {
      effectiveSubtotal = BigDecimal.ZERO;
      effectiveDeliveryFee = BigDecimal.ZERO;
      effectiveTotalAmount = BigDecimal.ZERO;
      effectiveTotalPaid = BigDecimal.ZERO;
      effectiveRemaining = BigDecimal.ZERO;
      effectiveIsPaid = false;
    }

    return new OrderResponse(
        order.getId(),
        orderCode,
        order.getClientOrderId(),
        customerName,
        customerPhone,
        tableId,
        numberTable,
        order.getOrderType(),
        DeliveryAddressDto.fromEntity(order.getDeliveryAddress()),
        order.getPaymentMethod(),
        order.getCashChangeFor(),
        effectiveSubtotal,
        effectiveDeliveryFee,
        effectiveTotalAmount,
        effectiveTotalPaid,
        effectiveRemaining,
        effectiveIsPaid,
        order.getOpenedAt(),
        order.getClosedAt(),
        order.getStatus(),
        order.getNotes(),
        order.getCancelReason(),
        order.getEstimatedMinutes(),
        order.getEstimatedDeliveryAt(),
        items
    );
  }
}
