package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.OrderPaymentRequest;
import com.restaurant.api.domain.dto.OrderPaymentStatusResponse;
import com.restaurant.api.domain.dto.PaymentItemDto;
import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.entity.Payment;
import com.restaurant.api.domain.entity.RestaurantConfig;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.enums.StatusPayment;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.OrderItemRepository;
import com.restaurant.api.domain.repository.OrderRepository;
import com.restaurant.api.domain.repository.PaymentRepository;
import com.restaurant.api.domain.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final RestaurantTableRepository tableRepository;
  private final RestaurantConfigService configService;

  public PaymentService(
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      RestaurantTableRepository tableRepository,
      RestaurantConfigService configService
  ) {
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.tableRepository = tableRepository;
    this.configService = configService;
  }

  @Transactional
  public OrderPaymentStatusResponse registerPayment(Long orderId, OrderPaymentRequest request) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Pedido não encontrado", "ID do pedido: " + orderId));

    if (order.getStatus() == StatusOrder.CLOSED || order.getStatus() == StatusOrder.CANCELLED) {
      throw new BusinessRulesException("Pedido finalizado", "Não é possível adicionar pagamentos a um pedido com status: " + order.getStatus());
    }

    BigDecimal totalOrderAmount = calculateOrderTotal(order);

    List<Payment> existingPayments = paymentRepository.findByOrderIdAndStatus(orderId, StatusPayment.APPROVED);
    BigDecimal alreadyPaid = existingPayments.stream()
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal remainingBefore = totalOrderAmount.subtract(alreadyPaid);
    if (remainingBefore.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRulesException("Conta já quitada", "Este pedido já está 100% pago.");
    }

    // Registra o pagamento
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setMethod(request.method());
    payment.setAmount(request.amount());
    payment.setStatus(StatusPayment.APPROVED);
    payment.setPayerName(request.payerName());
    payment.setPaymentDate(Instant.now());
    paymentRepository.save(payment);

    BigDecimal newTotalPaid = alreadyPaid.add(request.amount());
    BigDecimal newRemaining = totalOrderAmount.subtract(newTotalPaid);

    boolean isFullyPaid = newRemaining.compareTo(BigDecimal.ZERO) <= 0;

    // Se quitou totalmente, fecha o pedido e libera a mesa
    if (isFullyPaid) {
      order.close();
      if (order.getTable() != null) {
        order.getTable().availableTable();
        tableRepository.save(order.getTable());
      }
      orderRepository.save(order);

      // Cascata nos itens da comanda fechada
      List<com.restaurant.api.domain.entity.OrderItem> items = orderItemRepository.findByOrderId(orderId);
      if (items != null) {
        items.stream()
            .filter(item -> item.getStatus() != com.restaurant.api.domain.enums.StatusOrderItem.CANCELLED
                         && item.getStatus().ordinal() < com.restaurant.api.domain.enums.StatusOrderItem.DELIVERED.ordinal())
            .forEach(item -> {
              item.setStatus(com.restaurant.api.domain.enums.StatusOrderItem.DELIVERED);
              if (item.getDeliveredAt() == null) item.setDeliveredAt(Instant.now());
            });
        orderItemRepository.saveAll(items);
      }
    }

    return getOrderPaymentStatus(orderId);
  }

  public OrderPaymentStatusResponse getOrderPaymentStatus(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Pedido não encontrado", "ID do pedido: " + orderId));

    BigDecimal totalOrderAmount = calculateOrderTotal(order);

    List<Payment> payments = paymentRepository.findByOrderId(orderId);
    BigDecimal totalPaid = payments.stream()
        .filter(p -> p.getStatus() == StatusPayment.APPROVED)
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal remaining = totalOrderAmount.subtract(totalPaid);
    if (remaining.compareTo(BigDecimal.ZERO) < 0) {
      remaining = BigDecimal.ZERO;
    }

    boolean isFullyPaid = remaining.compareTo(BigDecimal.ZERO) <= 0;

    List<PaymentItemDto> paymentDtos = payments.stream()
        .map(PaymentItemDto::fromEntity)
        .toList();

    return new OrderPaymentStatusResponse(
        order.getId(),
        order.getOrderCode(),
        totalOrderAmount,
        totalPaid,
        remaining,
        isFullyPaid,
        paymentDtos
    );
  }

  public BigDecimal calculateOrderTotal(Order order) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    BigDecimal itemsTotal = items.stream()
        .map(item -> item.getUnitPrice() != null 
            ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())) 
            : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (order.getOrderType() == OrderType.DELIVERY) {
      RestaurantConfig config = configService.getConfigEntity();
      deliveryFee = config.getDeliveryFee() != null ? config.getDeliveryFee() : BigDecimal.valueOf(2.00);
    }

    return itemsTotal.add(deliveryFee);
  }
}
