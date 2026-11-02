package com.restaurant.api;

import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.enums.StatusPayment;
import com.restaurant.api.domain.repository.*;
import com.restaurant.api.domain.service.DailyOrderSequenceService;
import com.restaurant.api.domain.service.OrderService;
import com.restaurant.api.domain.service.RestaurantConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceBatchOptimizationTest {

  private OrderRepository orderRepository;
  private RestaurantTableRepository tableRepository;
  private ProductRepository productRepository;
  private OrderItemRepository orderItemRepository;
  private IngredientRepository ingredientRepository;
  private RestaurantConfigService configService;
  private PizzaSizeRepository pizzaSizeRepository;
  private ProductSizePriceRepository productSizePriceRepository;
  private UserAddressRepository userAddressRepository;
  private PaymentRepository paymentRepository;
  private DailyOrderSequenceService dailyOrderSequenceService;
  private UserRepository userRepository;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderRepository = mock(OrderRepository.class);
    tableRepository = mock(RestaurantTableRepository.class);
    productRepository = mock(ProductRepository.class);
    orderItemRepository = mock(OrderItemRepository.class);
    ingredientRepository = mock(IngredientRepository.class);
    configService = mock(RestaurantConfigService.class);
    pizzaSizeRepository = mock(PizzaSizeRepository.class);
    productSizePriceRepository = mock(ProductSizePriceRepository.class);
    userAddressRepository = mock(UserAddressRepository.class);
    paymentRepository = mock(PaymentRepository.class);
    dailyOrderSequenceService = mock(DailyOrderSequenceService.class);
    userRepository = mock(UserRepository.class);

    orderService = new OrderService(
        orderRepository,
        tableRepository,
        productRepository,
        orderItemRepository,
        ingredientRepository,
        configService,
        pizzaSizeRepository,
        productSizePriceRepository,
        userAddressRepository,
        paymentRepository,
        dailyOrderSequenceService,
        userRepository
    );

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(2.00));
    when(configService.getConfigEntity()).thenReturn(config);
  }

  @Test
  @DisplayName("Deve carregar itens e pagamentos em lote em findAll eliminando N+1")
  void testFindAllExecutesBatchFetchWithoutNPlusOne() {
    Order order1 = new Order();
    order1.setId(101L);
    order1.setStatus(StatusOrder.OPENED);
    order1.setOpenedAt(Instant.now());

    Order order2 = new Order();
    order2.setId(102L);
    order2.setStatus(StatusOrder.PREPARING);
    order2.setOpenedAt(Instant.now());

    Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), PageRequest.of(0, 10), 2);
    when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(orderPage);

    Product prod = new Product();
    prod.setId(1L);
    prod.setName("Pizza Muçarela");
    prod.setPrice(BigDecimal.valueOf(40.00));

    OrderItem item1 = new OrderItem();
    item1.setId(1L);
    item1.setOrder(order1);
    item1.setProduct(prod);
    item1.setQuantity(1);
    item1.setUnitPrice(BigDecimal.valueOf(40.00));

    OrderItem item2 = new OrderItem();
    item2.setId(2L);
    item2.setOrder(order2);
    item2.setProduct(prod);
    item2.setQuantity(2);
    item2.setUnitPrice(BigDecimal.valueOf(40.00));

    when(orderItemRepository.findByOrderIdInWithDetails(List.of(101L, 102L))).thenReturn(List.of(item1, item2));

    Payment payment2 = new Payment();
    payment2.setId(10L);
    payment2.setOrder(order2);
    payment2.setAmount(BigDecimal.valueOf(80.00));
    payment2.setStatus(StatusPayment.APPROVED);

    when(paymentRepository.findByOrderIdInAndStatus(List.of(101L, 102L), StatusPayment.APPROVED))
        .thenReturn(List.of(payment2));

    Page<OrderResponse> result = orderService.findAll(null, null, null, null, PageRequest.of(0, 10));

    assertEquals(2, result.getContent().size());
    assertEquals(1, result.getContent().get(0).items().size());
    assertEquals(1, result.getContent().get(1).items().size());

    // Verificações determinísticas de eliminação de N+1:
    verify(orderItemRepository, times(1)).findByOrderIdInWithDetails(List.of(101L, 102L));
    verify(paymentRepository, times(1)).findByOrderIdInAndStatus(List.of(101L, 102L), StatusPayment.APPROVED);
    verify(configService, times(1)).getConfigEntity();

    // Confirmação de que as chamadas individuais do loop NUNCA foram executadas:
    verify(orderItemRepository, never()).findByOrderId(anyLong());
    verify(orderRepository, never()).findById(anyLong());
  }
}
