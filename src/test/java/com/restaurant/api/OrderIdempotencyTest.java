package com.restaurant.api;

import com.restaurant.api.domain.controller.OrderController;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.entity.RestaurantConfig;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.repository.*;
import com.restaurant.api.domain.service.DailyOrderSequenceService;
import com.restaurant.api.domain.service.OrderService;
import com.restaurant.api.domain.service.RestaurantConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderIdempotencyTest {

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
  private PlatformTransactionManager transactionManager;

  private OrderService orderService;
  private OrderController orderController;

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
    transactionManager = mock(PlatformTransactionManager.class);

    when(transactionManager.getTransaction(any(TransactionDefinition.class)))
        .thenReturn(new SimpleTransactionStatus());

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(2.00));
    when(configService.getConfigEntity()).thenReturn(config);
    when(dailyOrderSequenceService.getBusinessDate(any())).thenReturn(LocalDate.now());
    when(dailyOrderSequenceService.nextSequence(any())).thenReturn(1);
    when(orderItemRepository.findByOrderId(any())).thenReturn(Collections.emptyList());
    when(paymentRepository.findByOrderIdAndStatus(any(), any())).thenReturn(Collections.emptyList());

    orderService = new OrderService(
        orderRepository, tableRepository, productRepository, orderItemRepository,
        ingredientRepository, configService, pizzaSizeRepository, productSizePriceRepository,
        userAddressRepository, paymentRepository, dailyOrderSequenceService, userRepository,
        transactionManager
    );

    orderController = new OrderController(orderService);
  }

  @Test
  @DisplayName("1. Idempotência sequencial: se clientOrderId já existe, retorna o pedido existente sem invocar save/saveAndFlush")
  void testSequentialDuplicateReturnsExistingOrderWithoutSaving() {
    String clientOrderId = "UUID-CLIENT-12345";
    Order existingOrder = new Order();
    existingOrder.setId(42L);
    existingOrder.setClientOrderId(clientOrderId);
    existingOrder.setOrderCode("#01");
    existingOrder.setStatus(StatusOrder.OPENED);

    when(orderRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(existingOrder));
    when(orderRepository.findById(42L)).thenReturn(Optional.of(existingOrder));

    OrderRequest request = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.PIX, null, "Observacao",
        List.of(), null, null, clientOrderId,
        "Carlos Teste", "38999990000"
    );

    OrderResponse response = orderService.createOrder(request);

    assertNotNull(response);
    assertEquals(42L, response.id());
    assertEquals("#01", response.orderCode());
    assertEquals(clientOrderId, response.clientOrderId());

    verify(orderRepository, times(1)).findByClientOrderId(clientOrderId);
    verify(orderRepository, never()).saveAndFlush(any());
    verify(orderRepository, never()).save(any());
  }

  @Test
  @DisplayName("2. Corrida concorrente: captura DataIntegrityViolationException em saveAndFlush e recupera pedido já persistido")
  void testRaceConditionCollisionHandledTransparently() {
    String clientOrderId = "RACE-UUID-99999";
    Order winnerOrder = new Order();
    winnerOrder.setId(100L);
    winnerOrder.setClientOrderId(clientOrderId);
    winnerOrder.setOrderCode("#99");
    winnerOrder.setStatus(StatusOrder.OPENED);

    // Na primeira checagem (fast-path), simula que o pedido ainda não existia
    when(orderRepository.findByClientOrderId(clientOrderId))
        .thenReturn(Optional.empty()) // tentativa 1
        .thenReturn(Optional.of(winnerOrder)); // tentativa 2 (após colisão)

    when(orderRepository.findById(100L)).thenReturn(Optional.of(winnerOrder));

    // saveAndFlush dispara violação de chave única
    when(orderRepository.saveAndFlush(any(Order.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_orders_client_order_id"));

    OrderRequest request = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "Nota",
        List.of(), null, null, clientOrderId,
        "Cliente Concorrente", "38999991111"
    );

    OrderResponse response = orderService.createOrder(request);

    assertNotNull(response);
    assertEquals(100L, response.id());
    assertEquals("#99", response.orderCode());
    assertEquals(clientOrderId, response.clientOrderId());
  }

  @Test
  @DisplayName("3. Violação de integridade que NÃO é relacionada a clientOrderId deve ser relançada normalmente")
  void testNonClientOrderIdConstraintViolationIsRethrown() {
    // Pedido sem clientOrderId
    when(orderRepository.saveAndFlush(any(Order.class)))
        .thenThrow(new DataIntegrityViolationException("null value in column user_id violates not-null constraint"));

    OrderRequest request = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "Nota",
        List.of(), null, null, null,
        "Cliente Sem ClientId", null
    );

    assertThrows(DataIntegrityViolationException.class, () -> orderService.createOrder(request));
  }

  @Test
  @DisplayName("4. Sanitização de clientOrderId em Order: poda espaços e converte vazio em null")
  void testClientOrderIdSanitizationInOrderEntity() {
    Order order = new Order();
    order.setClientOrderId("   UUID-WITH-SPACES   ");
    assertEquals("UUID-WITH-SPACES", order.getClientOrderId());

    order.setClientOrderId("    ");
    assertNull(order.getClientOrderId());

    order.setClientOrderId("");
    assertNull(order.getClientOrderId());

    order.setClientOrderId(null);
    assertNull(order.getClientOrderId());
  }

  @Test
  @DisplayName("5. Concorrência simulada multi-thread: 8 threads submetendo o mesmo clientOrderId")
  void testMultithreadedConcurrentOrderCreationOnlyOnePersisted() throws InterruptedException, ExecutionException {
    String clientOrderId = "CONCURRENT-MULTI-THREAD-1";
    Order persistedOrder = new Order();
    persistedOrder.setId(777L);
    persistedOrder.setClientOrderId(clientOrderId);
    persistedOrder.setOrderCode("#77");
    persistedOrder.setStatus(StatusOrder.OPENED);

    when(orderRepository.findById(777L)).thenReturn(Optional.of(persistedOrder));

    AtomicInteger attemptCount = new AtomicInteger(0);

    // Simula que a primeira thread consegue salvar, e todas as subsequentes colidem
    when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
      if (attemptCount.incrementAndGet() == 1) {
        return persistedOrder;
      }
      throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_orders_client_order_id");
    });

    when(orderRepository.findByClientOrderId(clientOrderId)).thenAnswer(invocation -> {
      if (attemptCount.get() > 0) {
        return Optional.of(persistedOrder);
      }
      return Optional.empty();
    });

    int threadCount = 8;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(1);
    List<Future<OrderResponse>> futures = new ArrayList<>();

    OrderRequest request = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.PIX, null, "Teste multi",
        List.of(), null, null, clientOrderId,
        "Multi Thread User", "38999992222"
    );

    for (int i = 0; i < threadCount; i++) {
      futures.add(executor.submit(() -> {
        latch.await();
        return orderService.createOrder(request);
      }));
    }

    latch.countDown(); // Libera todas as threads simultaneamente

    for (Future<OrderResponse> future : futures) {
      OrderResponse res = future.get();
      assertNotNull(res);
      assertEquals(777L, res.id());
      assertEquals("#77", res.orderCode());
      assertEquals(clientOrderId, res.clientOrderId());
    }

    executor.shutdown();
  }

  @Test
  @DisplayName("6. OrderController: retorna 201 CREATED para novo pedido e 200 OK para pedido já existente")
  void testOrderControllerReturnsCreatedForNewAndOkForDuplicate() {
    String newClientId = "NEW-CLIENT-ID-1";
    String existingClientId = "EXISTING-CLIENT-ID-2";

    when(orderRepository.existsByClientOrderId(newClientId)).thenReturn(false);
    when(orderRepository.existsByClientOrderId(existingClientId)).thenReturn(true);

    Order order = new Order();
    order.setId(10L);
    order.setOrderCode("#10");
    order.setClientOrderId(newClientId);
    when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(order);
    when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

    Order existingOrder = new Order();
    existingOrder.setId(20L);
    existingOrder.setOrderCode("#20");
    existingOrder.setClientOrderId(existingClientId);
    when(orderRepository.findByClientOrderId(existingClientId)).thenReturn(Optional.of(existingOrder));
    when(orderRepository.findById(20L)).thenReturn(Optional.of(existingOrder));

    OrderRequest newReq = new OrderRequest(null, OrderType.TAKEOUT, null, null, false, PaymentMethod.PIX, null, null, List.of(), null, null, newClientId, null, null);
    OrderRequest existingReq = new OrderRequest(null, OrderType.TAKEOUT, null, null, false, PaymentMethod.PIX, null, null, List.of(), null, null, existingClientId, null, null);

    ResponseEntity<OrderResponse> newResp = orderController.register(null, newReq);
    assertEquals(HttpStatus.CREATED, newResp.getStatusCode());
    assertEquals(10L, newResp.getBody().id());

    ResponseEntity<OrderResponse> dupResp = orderController.register(null, existingReq);
    assertEquals(HttpStatus.OK, dupResp.getStatusCode());
    assertEquals(200, dupResp.getStatusCode().value());
    assertEquals(20L, dupResp.getBody().id());
  }
}
