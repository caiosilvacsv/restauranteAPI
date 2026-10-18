package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.report.*;
import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.entity.Payment;
import com.restaurant.api.domain.entity.RestaurantConfig;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.enums.StatusPayment;
import com.restaurant.api.domain.repository.OrderItemRepository;
import com.restaurant.api.domain.repository.OrderRepository;
import com.restaurant.api.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

  public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final OrderItemRepository orderItemRepository;
  private final RestaurantConfigService configService;
  private final DailyOrderSequenceService dailyOrderSequenceService;

  public ReportService(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      OrderItemRepository orderItemRepository,
      RestaurantConfigService configService,
      DailyOrderSequenceService dailyOrderSequenceService
  ) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.orderItemRepository = orderItemRepository;
    this.configService = configService;
    this.dailyOrderSequenceService = dailyOrderSequenceService;
  }

  /**
   * Gera o relatório diário e fechamento de caixa consolidado.
   * Se targetDate for nulo, utiliza a data operacional do expediente atual.
   */
  @Transactional(readOnly = true)
  public DailyReportResponse getDailyReport(LocalDate targetDate) {
    RestaurantConfig config = configService.getConfigEntity();

    LocalDate businessDate = (targetDate != null)
        ? targetDate
        : dailyOrderSequenceService.getBusinessDate(config);

    Instant[] shiftInterval = calculateShiftInterval(businessDate, config);
    Instant shiftStart = shiftInterval[0];
    Instant shiftEnd = shiftInterval[1];

    List<Order> orders = orderRepository.findByOpenedAtBetweenOrderByOpenedAtAsc(shiftStart, shiftEnd);

    // 1. Métricas Operacionais
    long totalOrdersCreated = orders.size();
    long cancelledOrders = orders.stream()
        .filter(o -> o.getStatus() == StatusOrder.CANCELLED)
        .count();

    List<Order> validOrders = orders.stream()
        .filter(o -> o.getStatus() != StatusOrder.CANCELLED)
        .toList();

    long completedOrders = validOrders.stream()
        .filter(o -> o.getStatus() == StatusOrder.CLOSED || o.getStatus() == StatusOrder.DELIVERED)
        .count();

    // Se nenhum pedido foi formalmente fechado ainda mas o expediente está ativo, consideramos válidos não cancelados
    long effectiveCompleted = completedOrders > 0 ? completedOrders : validOrders.size();

    List<Long> validOrderIds = validOrders.stream().map(Order::getId).toList();

    // 2. Pagamentos e Formas de Pagamento
    List<Payment> paidPayments = validOrderIds.isEmpty()
        ? Collections.emptyList()
        : paymentRepository.findByOrderIdInAndStatus(validOrderIds, StatusPayment.APPROVED);

    // Mapeamento de totais por forma de pagamento
    Map<PaymentMethod, BigDecimal> paymentTotals = new EnumMap<>(PaymentMethod.class);
    Map<PaymentMethod, Long> paymentCounts = new EnumMap<>(PaymentMethod.class);

    for (PaymentMethod method : PaymentMethod.values()) {
      paymentTotals.put(method, BigDecimal.ZERO);
      paymentCounts.put(method, 0L);
    }

    BigDecimal totalPaidFromPayments = BigDecimal.ZERO;
    Set<Long> ordersWithApprovedPayments = new HashSet<>();
    for (Payment p : paidPayments) {
      if (p.getMethod() != null && p.getAmount() != null) {
        paymentTotals.put(p.getMethod(), paymentTotals.get(p.getMethod()).add(p.getAmount()));
        paymentCounts.put(p.getMethod(), paymentCounts.get(p.getMethod()) + 1);
        totalPaidFromPayments = totalPaidFromPayments.add(p.getAmount());
        if (p.getOrder() != null && p.getOrder().getId() != null) {
          ordersWithApprovedPayments.add(p.getOrder().getId());
        }
      }
    }

    // 3. Taxa de Entrega e Canais
    BigDecimal deliveryFeePerOrder = config.getDeliveryFee() != null ? config.getDeliveryFee() : BigDecimal.valueOf(2.00);
    BigDecimal deliveryFeeTotal = BigDecimal.ZERO;

    Map<OrderType, BigDecimal> channelTotals = new EnumMap<>(OrderType.class);
    Map<OrderType, Long> channelCounts = new EnumMap<>(OrderType.class);
    for (OrderType type : OrderType.values()) {
      channelTotals.put(type, BigDecimal.ZERO);
      channelCounts.put(type, 0L);
    }

    // Busca itens para calcular faturamento real de produtos
    List<OrderItem> validItems = validOrderIds.isEmpty()
        ? Collections.emptyList()
        : orderItemRepository.findValidItemsByOrderIds(validOrderIds, StatusOrderItem.CANCELLED);

    Map<Long, BigDecimal> orderItemsTotalMap = new HashMap<>();
    for (OrderItem item : validItems) {
      Long orderId = item.getOrder().getId();
      BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : item.calculateUnitPrice();
      BigDecimal itemTotal = (unitPrice != null)
          ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1))
          : BigDecimal.ZERO;
      orderItemsTotalMap.put(orderId, orderItemsTotalMap.getOrDefault(orderId, BigDecimal.ZERO).add(itemTotal));
    }

    BigDecimal productsGrossTotal = BigDecimal.ZERO;
    for (Order o : validOrders) {
      BigDecimal itemsTotal = orderItemsTotalMap.getOrDefault(o.getId(), BigDecimal.ZERO);
      BigDecimal orderDeliveryFee = BigDecimal.ZERO;

      if (o.getOrderType() == OrderType.DELIVERY) {
        orderDeliveryFee = deliveryFeePerOrder;
        deliveryFeeTotal = deliveryFeeTotal.add(orderDeliveryFee);
      }

      BigDecimal orderTotal = itemsTotal.add(orderDeliveryFee);
      productsGrossTotal = productsGrossTotal.add(orderTotal);

      channelTotals.put(o.getOrderType(), channelTotals.get(o.getOrderType()).add(orderTotal));
      channelCounts.put(o.getOrderType(), channelCounts.get(o.getOrderType()) + 1);

      // Se o pedido não tem transação no gateway online, contabiliza na forma de pagamento do pedido
      if (!ordersWithApprovedPayments.contains(o.getId()) && o.getPaymentMethod() != null) {
        paymentTotals.put(o.getPaymentMethod(), paymentTotals.get(o.getPaymentMethod()).add(orderTotal));
        paymentCounts.put(o.getPaymentMethod(), paymentCounts.get(o.getPaymentMethod()) + 1);
      }
    }

    // Faturamento bruto: consolida produtos + taxas dos pedidos válidos e qualquer transação online excedente
    BigDecimal grossTotal = productsGrossTotal.max(totalPaidFromPayments);

    BigDecimal netTotal = grossTotal.subtract(deliveryFeeTotal).max(BigDecimal.ZERO);

    BigDecimal averageTicket = effectiveCompleted > 0
        ? grossTotal.divide(BigDecimal.valueOf(effectiveCompleted), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    // 4. Lista de Formas de Pagamento formatada
    List<PaymentMethodBreakdownDto> paymentBreakdown = new ArrayList<>();
    for (PaymentMethod method : PaymentMethod.values()) {
      BigDecimal amount = paymentTotals.get(method);
      long count = paymentCounts.get(method);
      BigDecimal pct = grossTotal.compareTo(BigDecimal.ZERO) > 0
          ? amount.multiply(BigDecimal.valueOf(100)).divide(grossTotal, 2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

      paymentBreakdown.add(new PaymentMethodBreakdownDto(
          method,
          getMethodDescription(method),
          amount.setScale(2, RoundingMode.HALF_UP),
          count,
          pct
      ));
    }

    // 5. Lista de Canais formatada
    List<ChannelBreakdownDto> channelBreakdown = new ArrayList<>();
    for (OrderType type : OrderType.values()) {
      BigDecimal amount = channelTotals.get(type);
      long count = channelCounts.get(type);
      BigDecimal pct = grossTotal.compareTo(BigDecimal.ZERO) > 0
          ? amount.multiply(BigDecimal.valueOf(100)).divide(grossTotal, 2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

      channelBreakdown.add(new ChannelBreakdownDto(
          type,
          getChannelDescription(type),
          amount.setScale(2, RoundingMode.HALF_UP),
          count,
          pct
      ));
    }

    // 6. Ranking de Produtos Mais Vendidos (Top 10)
    Map<Long, ProductSalesAggregator> productMap = new HashMap<>();
    for (OrderItem item : validItems) {
      if (item.getProduct() != null) {
        Long prodId = item.getProduct().getId();
        String prodName = item.getProduct().getName();
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : item.calculateUnitPrice();
        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal itemPrice = (unitPrice != null) ? unitPrice.multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;

        productMap.computeIfAbsent(prodId, k -> new ProductSalesAggregator(prodId, prodName))
            .addSales(qty, itemPrice);
      }
    }

    List<ProductSalesRankingDto> topProducts = productMap.values().stream()
        .sorted(Comparator.comparing(ProductSalesAggregator::getTotalAmount).reversed())
        .limit(10)
        .map(a -> new ProductSalesRankingDto(
            a.productId,
            a.productName,
            a.quantity,
            a.totalAmount.setScale(2, RoundingMode.HALF_UP)
        ))
        .toList();

    FinancialSummaryDto financial = new FinancialSummaryDto(
        grossTotal.setScale(2, RoundingMode.HALF_UP),
        deliveryFeeTotal.setScale(2, RoundingMode.HALF_UP),
        netTotal.setScale(2, RoundingMode.HALF_UP),
        averageTicket
    );

    OperationsSummaryDto operations = new OperationsSummaryDto(
        totalOrdersCreated,
        completedOrders,
        cancelledOrders
    );

    return new DailyReportResponse(
        businessDate,
        businessDate.format(DATE_FORMATTER),
        shiftStart,
        shiftEnd,
        financial,
        paymentBreakdown,
        channelBreakdown,
        operations,
        topProducts
    );
  }

  /**
   * Converte a data de negócio em intervalo de tempo considerando o horário de funcionamento.
   */
  public Instant[] calculateShiftInterval(LocalDate businessDate, RestaurantConfig config) {
    LocalTime openTime = config != null && config.getOpenTime() != null
        ? config.getOpenTime()
        : LocalTime.of(18, 0);

    LocalTime closeTime = config != null && config.getCloseTime() != null
        ? config.getCloseTime()
        : LocalTime.of(23, 30);

    ZonedDateTime shiftStart;
    ZonedDateTime shiftEnd;

    // Se o expediente cruza a meia-noite (ex: 18:00 às 02:00)
    if (closeTime.isBefore(openTime)) {
      // Início: no dia do expediente, após o encerramento do turno anterior com margem (ex: 02:00 + 2h = 04:00)
      shiftStart = businessDate.atTime(closeTime).atZone(DEFAULT_ZONE).plusHours(2);
      // Fim: no dia seguinte, após o encerramento do turno com tolerância de 2h
      shiftEnd = businessDate.plusDays(1).atTime(closeTime).atZone(DEFAULT_ZONE).plusHours(2);
    } else {
      // Expediente fecha no mesmo dia civil (ex: 15:00 às 23:00)
      // Início: início do dia comercial (00:00:00) para cobrir testes ou pré-vendas
      shiftStart = businessDate.atStartOfDay(DEFAULT_ZONE);
      // Fim: horário de fechamento + 2 horas de tolerância (avança para o dia seguinte de forma segura)
      shiftEnd = businessDate.atTime(closeTime).atZone(DEFAULT_ZONE).plusHours(2);
    }

    return new Instant[]{shiftStart.toInstant(), shiftEnd.toInstant()};
  }

  private String getMethodDescription(PaymentMethod method) {
    if (method == null) return "Outros";
    return switch (method) {
      case CASH -> "Dinheiro";
      case PIX -> "PIX";
      case CREDIT_CARD -> "Cartão de Crédito";
      case DEBIT_CARD -> "Cartão de Débito";
      case SUMUP -> "Maquininha SumUp";
      default -> method.name();
    };
  }

  private String getChannelDescription(OrderType type) {
    if (type == null) return "Outros";
    return switch (type) {
      case DINE_IN -> "Salão / Mesas";
      case DELIVERY -> "Delivery";
      case TAKEOUT -> "Retirada no Balcão";
      default -> type.name();
    };
  }

  private static class ProductSalesAggregator {
    final Long productId;
    final String productName;
    long quantity = 0;
    BigDecimal totalAmount = BigDecimal.ZERO;

    ProductSalesAggregator(Long productId, String productName) {
      this.productId = productId;
      this.productName = productName;
    }

    void addSales(int qty, BigDecimal amount) {
      this.quantity += qty;
      if (amount != null) {
        this.totalAmount = this.totalAmount.add(amount);
      }
    }

    BigDecimal getTotalAmount() {
      return totalAmount;
    }
  }
}
