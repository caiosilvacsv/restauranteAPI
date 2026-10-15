package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.OrderItemRequest;
import com.restaurant.api.domain.dto.OrderItemResponse;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.ModificationType;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.*;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.enums.StatusPayment;
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
      PaymentRepository paymentRepository
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
  }

  @Transactional
  public OrderResponse createOrder(OrderRequest request) {
    // 1. Valida se a loja está aberta
    RestaurantConfig config = configService.getConfigEntity();
    if (!config.isCurrentlyOpen()) {
      throw new BusinessRulesException("Restaurante Fechado", config.getClosedMessage());
    }

    Order order = new Order();
    OrderType type = request.orderType() != null ? request.orderType() : OrderType.DELIVERY;
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
        com.restaurant.api.domain.entity.UserAddress userAddr = userAddressRepository
            .findByIdAndUserIdAndActiveTrue(request.addressId(), authenticatedUser.getId())
            .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + request.addressId()));
        order.setDeliveryAddress(userAddr.toDeliveryAddress());
      } else if (request.deliveryAddress() != null) {
        order.setDeliveryAddress(request.deliveryAddress().toEntity());

        // Salva opcionalmente o endereço na carteira do usuário se solicitado
        if (Boolean.TRUE.equals(request.saveAddress()) && authenticatedUser != null) {
          com.restaurant.api.domain.entity.UserAddress newAddr = new com.restaurant.api.domain.entity.UserAddress();
          newAddr.setUser(authenticatedUser);
          newAddr.setStreet(request.deliveryAddress().street());
          newAddr.setNumber(request.deliveryAddress().number());
          newAddr.setNeighborhood(request.deliveryAddress().neighborhood());
          newAddr.setComplement(request.deliveryAddress().complement());
          newAddr.setReferencePoint(request.deliveryAddress().referencePoint());
          newAddr.setActive(true);
          newAddr.setIsDefault(false);
          userAddressRepository.save(newAddr);
        }
      }
    }

    Order savedOrder = orderRepository.save(order);

    // 4. Gera código sequencial formatado no padrão #XXXX (ex: #1042)
    String orderCode = String.format("#%04d", savedOrder.getId());
    savedOrder.setOrderCode(orderCode);
    savedOrder = orderRepository.save(savedOrder);

    // 5. Adiciona itens caso tenham sido enviados diretamente no request
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
      spec = spec.and((root, query, cb) -> cb.or(
          cb.greaterThanOrEqualTo(root.get("updatedAt"), since),
          cb.greaterThanOrEqualTo(root.get("openedAt"), since)
      ));
    }

    if (date != null) {
      ZoneId zone = ZoneId.of("America/Sao_Paulo");
      Instant startOfDay = date.atStartOfDay(zone).toInstant();
      Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();
      spec = spec.and((root, query, cb) -> cb.between(root.get("openedAt"), startOfDay, endOfDay));
    }

    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    if (tableId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("table").get("id"), tableId));
    }

    return orderRepository.findAll(spec, pageable).map(this::mapToOrderResponse);
  }

  public Page<OrderResponse> findMyOrders(Pageable pageable) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User user)) {
      throw new BusinessRulesException("Não autenticado", "É necessário estar autenticado para consultar seus pedidos.");
    }
    return orderRepository.findByUserIdOrderByOpenedAtDesc(user.getId(), pageable).map(this::mapToOrderResponse);
  }

  public OrderResponse findById(Long id) {
    Order order = findOrderById(id);

    // Validação estrita de propriedade para cliente comum
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      boolean isStaff = user.getRole() == com.restaurant.api.domain.enums.Role.ADMIN 
          || user.getRole() == com.restaurant.api.domain.enums.Role.WAITER;
      if (!isStaff) {
        if (order.getUser() == null || order.getUser().getId() != user.getId()) {
          throw new BusinessRulesException("Acesso negado", "Você não tem permissão para visualizar pedidos de outros clientes.");
        }
      }
    }

    return mapToOrderResponse(order);
  }

  public OrderResponse mapToOrderResponse(Order order) {
    List<OrderItemResponse> items = itemsList(order.getId());
    BigDecimal itemsTotal = items.stream()
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
      if (newStatus == com.restaurant.api.domain.enums.StatusOrder.CLOSED) {
        order.setClosedAt(java.time.Instant.now());
      }
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

    BigDecimal basePrice;

    // Tratamento de Pizza Meio a Meio (2 Sabores com Média Aritmética)
    if (request.secondProductId() != null) {
      if (pizzaSize != null && pizzaSize.getMaxFlavors() < 2) {
        throw new BusinessRulesException("Tamanho não permite 2 sabores", 
            "O tamanho " + pizzaSize.getName() + " permite no máximo " + pizzaSize.getMaxFlavors() + " sabor(es).");
      }

      Product secondProduct = productRepository.findById(request.secondProductId())
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

      BigDecimal priceFirst;
      BigDecimal priceSecond;

      if (pizzaSize != null) {
        priceFirst = productSizePriceRepository.findByProductIdAndPizzaSizeId(product.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(product.getPrice());

        priceSecond = productSizePriceRepository.findByProductIdAndPizzaSizeId(secondProduct.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(secondProduct.getPrice());
      } else {
        priceFirst = product.getPrice();
        priceSecond = secondProduct.getPrice();
      }

      // Média aritmética com teto (arredonda sempre para cima no próximo inteiro, ex: 10.01 -> 11.00; 45.50 -> 46.00)
      BigDecimal rawAverage = priceFirst.add(priceSecond)
          .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
      basePrice = rawAverage.setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.UNNECESSARY);
    } else {
      if (pizzaSize != null) {
        basePrice = productSizePriceRepository.findByProductIdAndPizzaSizeId(product.getId(), pizzaSize.getId())
            .map(com.restaurant.api.domain.entity.ProductSizePrice::getPrice)
            .orElse(product.getPrice());
      } else {
        basePrice = product.getPrice();
      }
    }

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
