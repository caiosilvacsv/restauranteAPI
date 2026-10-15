package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.InvoiceRequest;
import com.restaurant.api.domain.dto.InvoiceResponse;
import com.restaurant.api.domain.entity.Invoice;
import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.exception.InvalidStateException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.InvoiceRepository;
import com.restaurant.api.domain.repository.OrderItemRepository;
import com.restaurant.api.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceService {
  private final InvoiceRepository invoiceRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderRepository orderRepository;

  public InvoiceService(
      InvoiceRepository invoiceRepository,
      OrderItemRepository orderItemRepository,
      OrderRepository orderRepository
  ) {
    this.invoiceRepository = invoiceRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderRepository = orderRepository;
  }

  public InvoiceResponse createInvoice(Long orderId, InvoiceRequest request) {
    Order order = validateStatusOrder(orderId);

    if (invoiceRepository.existsByOrderId(orderId))
      throw new InvalidStateException("Invoice already exists", "Don`t possible modification this order : " + orderId);

    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

    if (items.isEmpty())
      throw new ConflictException("This order does not exist items", "Don`t possible closed this order : " + orderId);

    List<OrderItem> itemsToDelivery = orderItemRepository
        .findByOrderIdAndStatusNot(orderId, StatusOrderItem.DELIVERED);

    if (!itemsToDelivery.isEmpty())
      throw new InvalidStateException(
          "Items status invalid",
          "It is necessary that all items have the status of delivered to close an order."
      );

    BigDecimal subtotalAmount = items
        .stream()
        .map(item -> item
            .getUnitPrice()
            .multiply(
                BigDecimal.valueOf(item.getQuantity()
                )))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    Invoice invoice = getInvoice(request, subtotalAmount, order);
    order.close();
    Invoice savedInvoice = invoiceRepository.save(invoice);
    orderRepository.save(order);
    return InvoiceResponse.fromEntity(savedInvoice);

  }

  public InvoiceResponse findByOrder(Long orderId) {
    Invoice invoice = invoiceRepository.findByOrderId(orderId)
        .orElseThrow(() -> new NotFoundException(
            "Order not found",
            "Order not found with id " + orderId
        ));
    return InvoiceResponse.fromEntity(invoice);
  }

  private static Invoice getInvoice(InvoiceRequest request, BigDecimal subtotalAmount, Order order) {
    BigDecimal serviceFee = request
        .serviceFee() != null ? request.serviceFee() : BigDecimal.ZERO;
    if (serviceFee.compareTo(BigDecimal.ZERO) < 0)
      throw new BusinessRulesException("Service fee is negative", "The fee is invalid for this order");

    BigDecimal discount = request
        .discount() != null ? request.discount() : BigDecimal.ZERO;
    if (discount.compareTo(BigDecimal.ZERO) < 0)
      throw new BusinessRulesException("Discount is negative", "The discount is invalid for this order");

    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (request != null && request.deliveryFee() != null) {
      deliveryFee = request.deliveryFee();
    } else if (order.getOrderType() == com.restaurant.api.domain.enums.OrderType.DELIVERY) {
      deliveryFee = BigDecimal.valueOf(2.00);
    }

    BigDecimal total = subtotalAmount.add(deliveryFee).add(serviceFee).subtract(discount);
    if (total.compareTo(BigDecimal.ZERO) < 0)
      throw new BusinessRulesException("Total is negative", "The total is invalid for this order");

    Invoice invoice = new Invoice();
    invoice.setOrder(order);
    invoice.setSubtotal(subtotalAmount);
    invoice.setDeliveryFee(deliveryFee);
    invoice.setServiceFee(serviceFee);
    invoice.setDiscount(discount);
    invoice.setTotalAmount(total);
    return invoice;
  }

  private Order findOrderById(Long orderId){
    return orderRepository.findById(orderId)
        .orElseThrow(()-> new NotFoundException("Order not found",
            "Order not found with ID: " + orderId));
  }

  private Order validateStatusOrder(Long orderId){
    Order order = findOrderById(orderId);

    if (order.getStatus().isFinished())
      throw new InvalidStateException(
          "Order is  closed or canceled",
          "Don`t possible access to this order : " + orderId);
    return order;
  }

}

