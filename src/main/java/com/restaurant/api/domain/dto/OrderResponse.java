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

    return new OrderResponse(
        order.getId(),
        orderCode,
        customerName,
        customerPhone,
        tableId,
        numberTable,
        order.getOrderType(),
        DeliveryAddressDto.fromEntity(order.getDeliveryAddress()),
        order.getPaymentMethod(),
        order.getCashChangeFor(),
        subtotal,
        deliveryFee,
        totalAmount,
        totalPaid != null ? totalPaid : BigDecimal.ZERO,
        remainingAmount != null ? remainingAmount : BigDecimal.ZERO,
        isFullyPaid != null ? isFullyPaid : false,
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
