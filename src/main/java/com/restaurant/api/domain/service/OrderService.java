package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.OrderEstimateRequest;
import com.restaurant.api.domain.dto.OrderGeneralUpdateRequest;
import com.restaurant.api.domain.dto.OrderItemRequest;
import com.restaurant.api.domain.dto.OrderItemResponse;
import com.restaurant.api.domain.dto.OrderPaymentMethodUpdateRequest;
import com.restaurant.api.domain.dto.OrderPaymentRequest;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.ModificationType;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.enums.Role;
import com.restaurant.api.domain.enums.StatusPayment;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final RestaurantTableRepository tableRepository;
  private final ProductRepository productRepository;
  private final OrderItemRepository orderItemRepository;
  private final IngredientRepository ingredientRepository;
  private final RestaurantConfigService configService;
  private final com.restaurant.api.domain.repository.PizzaSizeRepository pizzaSizeRepository;
  private final com.restaurant.api.domain.repository.ProductSizePriceRepository productSizePriceRepository;
  private final com.restaurant.api.domain.repository.UserAddressRepository userAddressRepository;
  private final PaymentRepository paymentRepository;
  private final DailyOrderSequenceService dailyOrderSequenceService;
  private final UserRepository userRepository;

  public OrderService(
      OrderRepository orderRepository,
      RestaurantTableRepository tableRepository,
      ProductRepository productRepository,
      OrderItemRepository orderItemRepository,
      IngredientRepository ingredientRepository,
      RestaurantConfigService configService,
      com.restaurant.api.domain.repository.PizzaSizeRepository pizzaSizeRepository,
      com.restaurant.api.domain.repository.ProductSizePriceRepository productSizePriceRepository,
      com.restaurant.api.domain.repository.UserAddressRepository userAddressRepository,
      PaymentRepository paymentRepository,
      DailyOrderSequenceService dailyOrderSequenceService,
      UserRepository userRepository
  ) {
    this.orderRepository = orderRepository;
    this.tableRepository = tableRepository;
    this.productRepository = productRepository;
    this.orderItemRepository = orderItemRepository;
    this.ingredientRepository = ingredientRepository;
    this.configService = configService;
    this.pizzaSizeRepository = pizzaSizeRepository;
    this.productSizePriceRepository = productSizePriceRepository;
    this.userAddressRepository = userAddressRepository;
    this.paymentRepository = paymentRepository;
    this.dailyOrderSequenceService = dailyOrderSequenceService;
    this.userRepository = userRepository;
  }

  @Transactional
  public OrderResponse createOrder(OrderRequest request) {
    // 0. Idempotência: se já existir pedido com o mesmo clientOrderId, retorna o pedido existente sem duplicar
    if (request.clientOrderId() != null && !request.clientOrderId().isBlank()) {
      java.util.Optional<Order> existing = orderRepository.findByClientOrderId(request.clientOrderId().trim());
      if (existing.isPresent()) {
        return findById(existing.get().getId());
      }
    }

    RestaurantConfig config = configService.getConfigEntity();

    // 1. Instancia o pedido base
    Order order = new Order();
    if (request.clientOrderId() != null && !request.clientOrderId().isBlank()) {
      order.setClientOrderId(request.clientOrderId().trim());
    }
    OrderType type = request.orderType() != null ? request.orderType() : OrderType.TAKEOUT;
    order.setOrderType(type);
    order.setNotes(request.note());
    order.setPaymentMethod(request.paymentMethod());
    order.setCashChangeFor(request.cashChangeFor());

    // 2. Se for pedido de salão (DINE_IN) ou foi informada mesa
    if (request.tableId() != null) {
      com.restaurant.api.domain.entity.RestaurantTables table = tableRepository.findById(request.tableId())
          .orElseThrow(() -> new NotFoundException("Mesa não encontrada", "ID: " + request.tableId()));
      table.setStatus(com.restaurant.api.domain.enums.StatusTable.OCCUPIED);
      tableRepository.save(table);
      order.setTable(table);
    }

    // 3. Vincula usuário autenticado se presente
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User authenticatedUser = null;
    if (auth != null && auth.getPrincipal() instanceof User user) {
      authenticatedUser = user;
      order.setUser(user);
    }

    // Resolução de endereço de entrega
    if (order.getOrderType() == OrderType.DELIVERY) {
      if (request.addressId() != null) {
        if (authenticatedUser == null) {
          throw new BusinessRulesException("Autenticação necessária", "Para selecionar um endereço salvo por ID é preciso estar logado.");
        }
        UserAddress userAddress = userAddressRepository.findByIdAndUserIdAndActiveTrue(request.addressId(), authenticatedUser.getId())
            .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + request.addressId()));

        DeliveryAddress deliveryAddress = new DeliveryAddress();
        deliveryAddress.setStreet(userAddress.getStreet());
        deliveryAddress.setNumber(userAddress.getNumber());
        deliveryAddress.setNeighborhood(userAddress.getNeighborhood());
        deliveryAddress.setComplement(userAddress.getComplement());
        deliveryAddress.setReferencePoint(userAddress.getReferencePoint());
        order.setDeliveryAddress(deliveryAddress);
      } else if (request.deliveryAddress() != null) {
        DeliveryAddress deliveryAddress = new DeliveryAddress();
        deliveryAddress.setStreet(request.deliveryAddress().street());
        deliveryAddress.setNumber(request.deliveryAddress().number());
        deliveryAddress.setNeighborhood(request.deliveryAddress().neighborhood());
        deliveryAddress.setComplement(request.deliveryAddress().complement());
        deliveryAddress.setReferencePoint(request.deliveryAddress().referencePoint());
        order.setDeliveryAddress(deliveryAddress);

        // Se o cliente autenticado marcou para salvar o endereço, cadastra na tabela user_addresses
        if (authenticatedUser != null && Boolean.TRUE.equals(request.saveAddress())) {
          UserAddress newAddr = new UserAddress();
          newAddr.setUser(authenticatedUser);
          newAddr.setNickname("Endereço de Entrega");
          newAddr.setStreet(deliveryAddress.getStreet());
          newAddr.setNumber(deliveryAddress.getNumber());
          newAddr.setNeighborhood(deliveryAddress.getNeighborhood());
          newAddr.setComplement(deliveryAddress.getComplement());
          newAddr.setReferencePoint(deliveryAddress.getReferencePoint());
          newAddr.setIsDefault(false);
          newAddr.setActive(true);
          userAddressRepository.save(newAddr);
        }
      }
    }

    // 4. Código diário do pedido
    if (request.orderCode() != null && !request.orderCode().isBlank()) {
      order.setOrderCode(request.orderCode().trim().toUpperCase());
    } else {
      LocalDate businessDate = dailyOrderSequenceService.getBusinessDate(config);
      int seq = dailyOrderSequenceService.nextSequence(businessDate);
      order.setOrderCode(String.format("#%02d", seq));
    }

    // Define estimativa inicial de entrega baseada na configuração geral da loja
    if (config.getEstimatedTimeMinutes() != null) {
      order.setEstimatedMinutes(config.getEstimatedTimeMinutes());
      order.setEstimatedDeliveryAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(config.getEstimatedTimeMinutes())));
    }

    // Resolução de dados do cliente (snapshot no pedido e autocura de conta)
    String resolvedCustomerName = null;
    if (request.customerName() != null && !request.customerName().trim().isBlank() && !request.customerName().trim().equalsIgnoreCase("Cliente")) {
      resolvedCustomerName = request.customerName().trim();
    } else if (request.payments() != null) {
      for (OrderPaymentRequest pay : request.payments()) {
        if (pay != null && pay.payerName() != null && !pay.payerName().trim().isBlank() && !pay.payerName().trim().equalsIgnoreCase("Cliente")) {
          resolvedCustomerName = pay.payerName().trim();
          break;
        }
      }
    }

    if (resolvedCustomerName == null && authenticatedUser != null && authenticatedUser.getRole() != Role.ADMIN && authenticatedUser.getRole() != Role.WAITER) {
      if (authenticatedUser.getName() != null && !authenticatedUser.getName().isBlank() && !authenticatedUser.getName().equalsIgnoreCase("Cliente")) {
        resolvedCustomerName = authenticatedUser.getName().trim();
      }
    }

    String resolvedCustomerPhone = null;
    if (request.customerPhone() != null && !request.customerPhone().trim().isBlank()) {
      resolvedCustomerPhone = request.customerPhone().trim();
    } else if (authenticatedUser != null && authenticatedUser.getNumber() != null && !authenticatedUser.getNumber().isBlank()) {
      resolvedCustomerPhone = authenticatedUser.getNumber().trim();
    }

    order.setCustomerName(resolvedCustomerName);
    order.setCustomerPhone(resolvedCustomerPhone);

    // Autocura de cadastro de cliente se seu nome estiver nulo ou "Cliente"
    if (authenticatedUser != null && authenticatedUser.getRole() != Role.ADMIN && authenticatedUser.getRole() != Role.WAITER && resolvedCustomerName != null) {
      if (authenticatedUser.getName() == null || authenticatedUser.getName().isBlank() || authenticatedUser.getName().equalsIgnoreCase("Cliente")) {
        authenticatedUser.setName(resolvedCustomerName);
        userRepository.save(authenticatedUser);
      }
    }

    Order savedOrder = orderRepository.save(order);

    // 5. Adiciona itens ao pedido
    List<OrderItemResponse> itemResponses = new ArrayList<>();
    BigDecimal itemsTotal = BigDecimal.ZERO;
    if (request.items() != null && !request.items().isEmpty()) {
      for (OrderItemRequest itemReq : request.items()) {
        OrderItemResponse added = addItem(savedOrder.getId(), itemReq);
        itemResponses.add(added);
        if (added.total() != null) {
          itemsTotal = itemsTotal.add(added.total());
        }
      }
    }

    // 6. Calcula taxa de entrega e total
    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (savedOrder.getOrderType() == OrderType.DELIVERY) {
      deliveryFee = config.getDeliveryFee() != null ? config.getDeliveryFee() : BigDecimal.valueOf(2.00);
    }
    BigDecimal totalAmount = itemsTotal.add(deliveryFee);

    // Validação de troco para dinheiro
    if (savedOrder.getPaymentMethod() == com.restaurant.api.domain.enums.PaymentMethod.CASH && savedOrder.getCashChangeFor() != null) {
      if (savedOrder.getCashChangeFor().compareTo(totalAmount) < 0) {
        throw new BusinessRulesException("Valor de troco inválido", 
            "O valor para troco (R$ " + savedOrder.getCashChangeFor() + ") não pode ser menor que o total do pedido (R$ " + totalAmount + ").");
      }
    }

    // 7. Persistência de pagamentos divididos/múltiplos no ato da criação
    if (request.payments() != null && !request.payments().isEmpty()) {
      for (OrderPaymentRequest payReq : request.payments()) {
        if (payReq != null && payReq.amount() != null && payReq.amount().compareTo(BigDecimal.ZERO) > 0) {
          Payment payment = new Payment();
          payment.setOrder(savedOrder);
          payment.setMethod(payReq.method());
          payment.setAmount(payReq.amount());
          payment.setStatus(StatusPayment.APPROVED);
          String payer = payReq.payerName() != null && !payReq.payerName().trim().isBlank() ? payReq.payerName().trim() : (savedOrder.getCustomerName() != null ? savedOrder.getCustomerName() : (savedOrder.getUser() != null ? savedOrder.getUser().getName() : "Cliente"));
          payment.setPayerName(payer);
          payment.setPaymentDate(Instant.now());
          paymentRepository.save(payment);
        }
      }
    }

    return mapToOrderResponse(savedOrder);
  }

  public Page<OrderResponse> findAll(Pageable pageable) {
    return findAll(null, null, null, null, pageable);
  }

  public Page<OrderResponse> findAll(
      Instant since,
      LocalDate date,
      com.restaurant.api.domain.enums.StatusOrder status,
      Long tableId,
      Pageable pageable
  ) {
    Specification<Order> spec = (root, query, cb) -> cb.conjunction();

    if (since != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("openedAt"), since));
    }

    if (date != null) {
      RestaurantConfig config = configService.getConfigEntity();
      Instant[] shiftInterval = calculateShiftInterval(date, config);
      Instant start = shiftInterval[0];
      Instant end = shiftInterval[1];
      spec = spec.and((root, query, cb) -> cb.between(root.get("openedAt"), start, end));
    }

    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    if (tableId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("table").get("id"), tableId));
    }

    return orderRepository.findAll(spec, pageable).map(this::mapToOrderResponse);
  }

  private Instant[] calculateShiftInterval(LocalDate businessDate, RestaurantConfig config) {
    ZoneId zone = ZoneId.of("America/Sao_Paulo");
    java.time.LocalTime openTime = config != null && config.getOpenTime() != null ? config.getOpenTime() : java.time.LocalTime.of(18, 0);
    java.time.LocalTime closeTime = config != null && config.getCloseTime() != null ? config.getCloseTime() : java.time.LocalTime.of(23, 30);

    java.time.ZonedDateTime shiftStart;
    java.time.ZonedDateTime shiftEnd;

    if (closeTime.isBefore(openTime)) {
      shiftStart = businessDate.atTime(closeTime).atZone(zone).plusHours(2);
      shiftEnd = businessDate.plusDays(1).atTime(closeTime).atZone(zone).plusHours(2);
    } else {
      shiftStart = businessDate.atStartOfDay(zone);
      shiftEnd = businessDate.atTime(closeTime).atZone(zone).plusHours(2);
    }

    return new Instant[]{shiftStart.toInstant(), shiftEnd.toInstant()};
  }

  public Page<OrderResponse> findMyOrders(Pageable pageable) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User user)) {
      throw new BusinessRulesException("Acesso não autorizado", "É necessário estar autenticado para consultar seus pedidos.");
    }

    return orderRepository.findByUserIdOrderByOpenedAtDesc(user.getId(), pageable)
        .map(this::mapToOrderResponse);
  }

  public OrderResponse findById(Long id) {
    Order order = findOrderById(id);
    validarAcessoPedido(order);
    return mapToOrderResponse(order);
  }

  private void validarAcessoPedido(Order order) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
      throw new org.springframework.security.access.AccessDeniedException("Acesso negado: faça login para visualizar este pedido.");
    }

    boolean isStaff = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_WAITER"));
    if (isStaff) {
      return;
    }

    if (order.getUser() != null) {
      if (auth.getPrincipal() instanceof User currentUser) {
        if (order.getUser().getId() != currentUser.getId()) {
          throw new org.springframework.security.access.AccessDeniedException("Acesso negado: este pedido pertence a outro usuário.");
        }
      } else {
        String currentLogin = auth.getName();
        if (!order.getUser().getLogin().equals(currentLogin)) {
          throw new org.springframework.security.access.AccessDeniedException("Acesso negado: este pedido pertence a outro usuário.");
        }
      }
    }
  }

  public OrderResponse mapToOrderResponse(Order order) {
    List<OrderItemResponse> items = itemsList(order.getId());
    BigDecimal itemsTotal = items.stream()
        .filter(it -> it.status() != StatusOrderItem.CANCELLED)
        .map(OrderItemResponse::total)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    RestaurantConfig config = configService.getConfigEntity();
    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (order.getOrderType() == OrderType.DELIVERY) {
      deliveryFee = config.getDeliveryFee() != null ? config.getDeliveryFee() : BigDecimal.valueOf(2.00);
    }
    BigDecimal totalAmount = itemsTotal.add(deliveryFee);

    List<Payment> approvedPayments = paymentRepository.findByOrderIdAndStatus(order.getId(), StatusPayment.APPROVED);
    BigDecimal totalPaid = approvedPayments.stream()
        .map(Payment::getAmount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal remainingAmount = totalAmount.subtract(totalPaid);
    if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
      remainingAmount = BigDecimal.ZERO;
    }

    boolean isFullyPaid = order.getStatus() == com.restaurant.api.domain.enums.StatusOrder.CLOSED 
        || (totalAmount.compareTo(BigDecimal.ZERO) > 0 && remainingAmount.compareTo(BigDecimal.ZERO) <= 0);

    return OrderResponse.fromEntity(
        order,
        items,
        itemsTotal,
        deliveryFee,
        totalAmount,
        totalPaid,
        remainingAmount,
        isFullyPaid
    );
  }

  public BigDecimal calculateOrderTotal(Order order) {
    List<OrderItemResponse> items = itemsList(order.getId());
    BigDecimal itemsTotal = items.stream()
        .filter(it -> it.status() != StatusOrderItem.CANCELLED)
        .map(OrderItemResponse::total)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    RestaurantConfig config = configService.getConfigEntity();
    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (order.getOrderType() == OrderType.DELIVERY) {
      deliveryFee = config.getDeliveryFee() != null ? config.getDeliveryFee() : BigDecimal.valueOf(2.00);
    }
    return itemsTotal.add(deliveryFee);
  }

  private void validateUserCanModifyOrder(Order order) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      boolean isStaff = user.getRole() == com.restaurant.api.domain.enums.Role.ADMIN 
          || user.getRole() == com.restaurant.api.domain.enums.Role.WAITER;
      if (!isStaff) {
        if (order.getUser() == null || order.getUser().getId() != user.getId()) {
          throw new BusinessRulesException("Acesso negado", "Você não tem permissão para alterar este pedido.");
        }
      }
    }
  }

  private void cascadeStatusToItems(Order order, com.restaurant.api.domain.enums.StatusOrder newStatus) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    if (items == null || items.isEmpty()) return;

    if (newStatus == com.restaurant.api.domain.enums.StatusOrder.CANCELLED) {
      items.stream()
          .filter(item -> item.getStatus() != StatusOrderItem.CANCELLED)
          .forEach(item -> item.setStatus(StatusOrderItem.CANCELLED));
      orderItemRepository.saveAll(items);
    } else if (newStatus == com.restaurant.api.domain.enums.StatusOrder.DESPATCHED) {
      items.stream()
          .filter(item -> item.getStatus() != StatusOrderItem.CANCELLED 
                       && item.getStatus().ordinal() < StatusOrderItem.DESPATCHED.ordinal())
          .forEach(item -> item.setStatus(StatusOrderItem.DESPATCHED));
      orderItemRepository.saveAll(items);
    } else if (newStatus == com.restaurant.api.domain.enums.StatusOrder.READY) {
      items.stream()
          .filter(item -> item.getStatus() != StatusOrderItem.CANCELLED 
                       && item.getStatus().ordinal() < StatusOrderItem.READY.ordinal())
          .forEach(item -> {
            item.setStatus(StatusOrderItem.READY);
            if (item.getReadyAt() == null) {
              item.isReady();
            }
          });
      orderItemRepository.saveAll(items);
    } else if (newStatus == com.restaurant.api.domain.enums.StatusOrder.DELIVERED || newStatus == com.restaurant.api.domain.enums.StatusOrder.CLOSED) {
      items.stream()
          .filter(item -> item.getStatus() != StatusOrderItem.CANCELLED 
                       && item.getStatus().ordinal() < StatusOrderItem.DELIVERED.ordinal())
          .forEach(item -> {
            item.setStatus(StatusOrderItem.DELIVERED);
            if (item.getDeliveredAt() == null) {
              item.delivered();
            }
          });
      orderItemRepository.saveAll(items);
    }
  }

  @Transactional
  public OrderResponse updateStatus(Long orderId, com.restaurant.api.domain.enums.StatusOrder newStatus) {
    Order order = findOrderById(orderId);
    order.setStatus(newStatus);

    cascadeStatusToItems(order, newStatus);

    if (newStatus == com.restaurant.api.domain.enums.StatusOrder.CLOSED || newStatus == com.restaurant.api.domain.enums.StatusOrder.CANCELLED) {
      if (order.getTable() != null) {
        order.getTable().availableTable();
        tableRepository.save(order.getTable());
      }
      order.setClosedAt(java.time.Instant.now());
    }

    Order updated = orderRepository.save(order);
    return mapToOrderResponse(updated);
  }

  @Transactional
  public OrderResponse updateEstimate(Long orderId, Integer estimatedMinutes) {
    Order order = findOrderById(orderId);
    order.setEstimatedMinutes(estimatedMinutes);
    order.setEstimatedDeliveryAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(estimatedMinutes)));
    Order updated = orderRepository.save(order);
    return mapToOrderResponse(updated);
  }

  @Transactional
  public OrderResponse cancelOrder(Long orderId, String reason) {
    Order order = findOrderById(orderId);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      boolean isStaff = user.getRole() == com.restaurant.api.domain.enums.Role.ADMIN 
          || user.getRole() == com.restaurant.api.domain.enums.Role.WAITER;
      
      if (!isStaff) {
        // Valida que o pedido pertence ao cliente
        if (order.getUser() == null || order.getUser().getId() != user.getId()) {
          throw new BusinessRulesException("Acesso negado", "Você não tem permissão para cancelar este pedido.");
        }
        // Cliente só pode cancelar enquanto OPENED
        if (order.getStatus() != com.restaurant.api.domain.enums.StatusOrder.OPENED) {
          throw new BusinessRulesException("Cancelamento não permitido", 
              "O pedido já está em " + order.getStatus() + " e não pode ser cancelado pelo aplicativo. Entre em contato com a pizzaria.");
        }
      }
    }

    order.setStatus(com.restaurant.api.domain.enums.StatusOrder.CANCELLED);
    order.setCancelReason(reason != null && !reason.isBlank() ? reason : "Cancelado pelo usuário");
    order.setClosedAt(java.time.Instant.now());

    cascadeStatusToItems(order, com.restaurant.api.domain.enums.StatusOrder.CANCELLED);

    if (order.getTable() != null) {
      order.getTable().availableTable();
      tableRepository.save(order.getTable());
    }

    Order saved = orderRepository.save(order);
    return mapToOrderResponse(saved);
  }

  @Transactional
  public OrderResponse updatePaymentMethod(Long orderId, OrderPaymentMethodUpdateRequest request) {
    Order order = findOrderById(orderId);
    validateUserCanModifyOrder(order);

    if (order.getStatus().isFinished()) {
      throw new BusinessRulesException("Pedido finalizado", "Não é possível alterar o meio de pagamento de um pedido com status: " + order.getStatus());
    }

    // Validação de troco se for CASH
    if (request.paymentMethod() == com.restaurant.api.domain.enums.PaymentMethod.CASH && request.cashChangeFor() != null) {
      BigDecimal totalAmount = calculateOrderTotal(order);
      if (request.cashChangeFor().compareTo(totalAmount) < 0) {
        throw new BusinessRulesException("Valor de troco inválido",
            "O valor para troco (R$ " + request.cashChangeFor() + ") não pode ser menor que o total do pedido (R$ " + totalAmount + ").");
      }
    }

    order.setPaymentMethod(request.paymentMethod());
    order.setCashChangeFor(request.paymentMethod() == com.restaurant.api.domain.enums.PaymentMethod.CASH ? request.cashChangeFor() : null);

    Order saved = orderRepository.save(order);
    return mapToOrderResponse(saved);
  }

  @Transactional
  public OrderResponse updateOrder(Long orderId, OrderGeneralUpdateRequest request) {
    Order order = findOrderById(orderId);
    validateUserCanModifyOrder(order);

    if (!order.getStatus().isOpen()) {
      throw new BusinessRulesException("Pedido não está aberto", "Não é possível alterar informações de um pedido em status: " + order.getStatus());
    }

    if (request.note() != null) {
      order.setNotes(request.note().trim());
    }

    // Alteração de mesa se enviada
    if (request.tableId() != null) {
      if (order.getTable() == null || !order.getTable().getId().equals(request.tableId())) {
        com.restaurant.api.domain.entity.RestaurantTables newTable = tableRepository.findById(request.tableId())
            .orElseThrow(() -> new NotFoundException("Mesa não encontrada", "ID da mesa: " + request.tableId()));

        if (order.getTable() != null) {
          order.getTable().availableTable();
          tableRepository.save(order.getTable());
        }

        newTable.setStatus(com.restaurant.api.domain.enums.StatusTable.OCCUPIED);
        tableRepository.save(newTable);
        order.setTable(newTable);
      }
    }

    // Alteração de endereço de entrega
    if (order.getOrderType() == OrderType.DELIVERY) {
      if (request.addressId() != null) {
        UserAddress userAddress = userAddressRepository.findById(request.addressId())
            .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID: " + request.addressId()));
        DeliveryAddress deliveryAddress = new DeliveryAddress();
        deliveryAddress.setStreet(userAddress.getStreet());
        deliveryAddress.setNumber(userAddress.getNumber());
        deliveryAddress.setNeighborhood(userAddress.getNeighborhood());
        deliveryAddress.setComplement(userAddress.getComplement());
        deliveryAddress.setReferencePoint(userAddress.getReferencePoint());
        order.setDeliveryAddress(deliveryAddress);
      } else if (request.deliveryAddress() != null) {
        DeliveryAddress deliveryAddress = new DeliveryAddress();
        deliveryAddress.setStreet(request.deliveryAddress().street());
        deliveryAddress.setNumber(request.deliveryAddress().number());
        deliveryAddress.setNeighborhood(request.deliveryAddress().neighborhood());
        deliveryAddress.setComplement(request.deliveryAddress().complement());
        deliveryAddress.setReferencePoint(request.deliveryAddress().referencePoint());
        order.setDeliveryAddress(deliveryAddress);
      }
    }

    Order saved = orderRepository.save(order);
    return mapToOrderResponse(saved);
  }

  @Transactional
  public OrderResponse removeItem(Long orderId, Long itemId) {
    Order order = findOrderById(orderId);
    validateUserCanModifyOrder(order);

    if (!order.getStatus().isOpen()) {
      throw new BusinessRulesException("Pedido não está aberto", "Não é possível remover itens de um pedido em status: " + order.getStatus());
    }

    OrderItem item = orderItemRepository.findById(itemId)
        .orElseThrow(() -> new NotFoundException("Item do pedido não encontrado", "ID do item: " + itemId));

    if (item.getOrder() == null || !item.getOrder().getId().equals(orderId)) {
      throw new BusinessRulesException("Item inválido", "O item " + itemId + " não pertence ao pedido " + orderId);
    }

    if (item.getStatus() == StatusOrderItem.DELIVERED || item.getStatus() == StatusOrderItem.CLOSED || item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item não pode ser removido", "O item já está em status " + item.getStatus());
    }

    if (item.getStatus() == StatusOrderItem.PENDING || item.getStatus() == StatusOrderItem.QUEUED) {
      orderItemRepository.delete(item);
    } else {
      item.setStatus(StatusOrderItem.CANCELLED);
      orderItemRepository.save(item);
    }

    return mapToOrderResponse(order);
  }

  @Transactional
  public OrderItemResponse updateItem(Long orderId, Long itemId, OrderItemRequest request) {
    Order order = findOrderById(orderId);
    validateUserCanModifyOrder(order);

    if (!order.getStatus().isOpen()) {
      throw new BusinessRulesException("Pedido não está aberto", "Não é possível editar itens de um pedido em status: " + order.getStatus());
    }

    OrderItem item = orderItemRepository.findById(itemId)
        .orElseThrow(() -> new NotFoundException("Item do pedido não encontrado", "ID do item: " + itemId));

    if (item.getOrder() == null || !item.getOrder().getId().equals(orderId)) {
      throw new BusinessRulesException("Item inválido", "O item " + itemId + " não pertence ao pedido " + orderId);
    }

    if (item.getStatus() == StatusOrderItem.DELIVERED || item.getStatus() == StatusOrderItem.CLOSED || item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item não pode ser editado", "O item já está em status: " + item.getStatus());
    }

    if (request.quantity() != null && request.quantity() > 0) {
      item.setQuantity(request.quantity());
    }
    if (request.note() != null) {
      item.setNotes(request.note().trim());
    }
    if (request.notesFirstHalf() != null) {
      item.setNotesFirstHalf(request.notesFirstHalf().trim());
    }
    if (request.notesSecondHalf() != null) {
      item.setNotesSecondHalf(request.notesSecondHalf().trim());
    }

    // Se novos adicionais ou remoções foram informados, recalcula as modificações
    if (request.addIngredients() != null || request.removeIngredients() != null) {
      item.getModifications().clear();
      BigDecimal finalUnitPrice = calculateBasePrice(item.getProduct(), item.getSecondProduct(), item.getPizzaSize());

      if (request.addIngredients() != null && !request.addIngredients().isEmpty()) {
        Set<Ingredient> ingredientsToAdd = ingredientRepository.findAllByIdInAndActiveTrue(request.addIngredients());
        for (Ingredient ingredient : ingredientsToAdd) {
          OrderItemModification mod = new OrderItemModification();
          mod.setOrderItem(item);
          mod.setIngredient(ingredient);
          mod.setModificationType(ModificationType.ADD);
          mod.setExtraPrice(ingredient.getPrice());
          mod.setQuantity(1);
          item.getModifications().add(mod);
          finalUnitPrice = finalUnitPrice.add(ingredient.getPrice());
        }
      }

      if (request.removeIngredients() != null && !request.removeIngredients().isEmpty()) {
        Set<Ingredient> ingredientsToRemove = ingredientRepository.findAllByIdInAndActiveTrue(request.removeIngredients());
        for (Ingredient ingredient : ingredientsToRemove) {
          OrderItemModification mod = new OrderItemModification();
          mod.setOrderItem(item);
          mod.setIngredient(ingredient);
          mod.setModificationType(ModificationType.REMOVE);
          mod.setExtraPrice(BigDecimal.ZERO);
          mod.setQuantity(1);
          item.getModifications().add(mod);
        }
      }

      item.setUnitPrice(finalUnitPrice);
    }

    OrderItem saved = orderItemRepository.save(item);
    return OrderItemResponse.fromEntity(saved);
  }

  public BigDecimal calculateBasePrice(Product product, Product secondProduct, com.restaurant.api.domain.entity.PizzaSize pizzaSize) {
    if (secondProduct != null) {
      BigDecimal defaultFirstPrice = (pizzaSize != null && pizzaSize.getPrice() != null && pizzaSize.getPrice().compareTo(BigDecimal.ZERO) > 0)
          ? pizzaSize.getPrice()
          : product.getPrice();

      BigDecimal defaultSecondPrice = (pizzaSize != null && pizzaSize.getPrice() != null && pizzaSize.getPrice().compareTo(BigDecimal.ZERO) > 0)
          ? pizzaSize.getPrice()
          : secondProduct.getPrice();

      BigDecimal priceFirst;
      BigDecimal priceSecond;
      if (pizzaSize != null) {
        priceFirst = productSizePriceRepository.findByProductIdAndPizzaSizeId(product.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(defaultFirstPrice);

        priceSecond = productSizePriceRepository.findByProductIdAndPizzaSizeId(secondProduct.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(defaultSecondPrice);
      } else {
        priceFirst = product.getPrice();
        priceSecond = secondProduct.getPrice();
      }

      BigDecimal rawAverage = priceFirst.add(priceSecond)
          .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
      return rawAverage.setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.UNNECESSARY);
    } else {
      if (pizzaSize != null) {
        BigDecimal defaultSizePrice = (pizzaSize.getPrice() != null && pizzaSize.getPrice().compareTo(BigDecimal.ZERO) > 0)
            ? pizzaSize.getPrice()
            : product.getPrice();

        return productSizePriceRepository.findByProductIdAndPizzaSizeId(product.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(defaultSizePrice);
      } else {
        return product.getPrice();
      }
    }
  }

  @Transactional
  public OrderItemResponse addItem(Long id, OrderItemRequest request) {
    if (request.quantity() == null || request.quantity() <= 0)
      throw new BusinessRulesException("Invalid quantity");

    if (request.productId() == null)
      throw new BusinessRulesException("Invalid product id");

    Order order = findOrderById(id);

    if (!order.getStatus().isOpen())
      throw new BusinessRulesException("Order is not opened");

    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new NotFoundException(
            "Product not found",
            "Product not found with ID: " + request.productId()
        ));

    if (!product.getActive())
      throw new BusinessRulesException("Product is not available in menu!");

    OrderItem items = new OrderItem();
    items.setOrder(order);
    items.setProduct(product);
    items.setQuantity(request.quantity());
    items.setNotes(request.note());

    // Se o tamanho foi informado, busca a entidade PizzaSize
    com.restaurant.api.domain.entity.PizzaSize pizzaSize = null;
    if (request.sizeId() != null) {
      pizzaSize = pizzaSizeRepository.findById(request.sizeId())
          .orElseThrow(() -> new NotFoundException("Tamanho de pizza não encontrado", "ID: " + request.sizeId()));
      if (!Boolean.TRUE.equals(pizzaSize.getActive())) {
        throw new BusinessRulesException("Tamanho indisponível", "O tamanho selecionado está inativo.");
      }
      items.setPizzaSize(pizzaSize);
    }

    Product secondProduct = null;
    if (request.secondProductId() != null) {
      if (pizzaSize != null && pizzaSize.getMaxFlavors() < 2) {
        throw new BusinessRulesException("Tamanho não permite 2 sabores", 
            "O tamanho " + pizzaSize.getName() + " permite no máximo " + pizzaSize.getMaxFlavors() + " sabor(es).");
      }

      secondProduct = productRepository.findById(request.secondProductId())
          .orElseThrow(() -> new NotFoundException(
              "Second product not found",
              "Second product not found with ID: " + request.secondProductId()
          ));

      if (!secondProduct.getActive())
        throw new BusinessRulesException("Second product is not available in menu!");

      if (!Boolean.TRUE.equals(product.getAllowsHalfAndHalf()) || !Boolean.TRUE.equals(secondProduct.getAllowsHalfAndHalf())) {
        throw new BusinessRulesException("Meio a Meio inválido", "Um ou ambos os sabores selecionados não estão habilitados para pizza meio a meio!");
      }

      items.setSecondProduct(secondProduct);
      items.setNotesFirstHalf(request.notesFirstHalf());
      items.setNotesSecondHalf(request.notesSecondHalf());
    }

    BigDecimal basePrice = calculateBasePrice(product, secondProduct, pizzaSize);
    BigDecimal finalUnitPrice = basePrice;

    // Process ingredients additions/removals
    if (request.addIngredients() != null && !request.addIngredients().isEmpty()) {
      Set<Ingredient> ingredientsToAdd = ingredientRepository
          .findAllByIdInAndActiveTrue(request.addIngredients());

      for (Ingredient ingredient : ingredientsToAdd) {
        OrderItemModification mod = new OrderItemModification();
        mod.setOrderItem(items);
        mod.setIngredient(ingredient);
        mod.setModificationType(ModificationType.ADD);
        mod.setExtraPrice(ingredient.getPrice());
        mod.setQuantity(1);
        items.getModifications().add(mod);
        finalUnitPrice = finalUnitPrice.add(ingredient.getPrice());
      }
    }

    if (request.removeIngredients() != null && !request.removeIngredients().isEmpty()) {
      Set<Ingredient> ingredientsToRemove = ingredientRepository
          .findAllByIdInAndActiveTrue(request.removeIngredients());
      for (Ingredient ingredient : ingredientsToRemove) {
        OrderItemModification mod = new OrderItemModification();
        mod.setOrderItem(items);
        mod.setIngredient(ingredient);
        mod.setModificationType(ModificationType.REMOVE);
        mod.setExtraPrice(BigDecimal.ZERO);
        mod.setQuantity(1);

        items.getModifications().add(mod);
      }
    }

    items.setUnitPrice(finalUnitPrice);
    OrderItem savedItem = orderItemRepository.save(items);

    return OrderItemResponse.fromEntity(savedItem);
  }

  public List<OrderItemResponse> itemsList(Long orderId) {
    Order order = findOrderById(orderId);
    return orderItemRepository.findByOrderId(order.getId()).stream().map(OrderItemResponse::fromEntity).toList();
  }

  private Order findOrderById(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found",
            "Order not found with ID: " + orderId));
  }
}
