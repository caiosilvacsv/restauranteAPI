package com.restaurant.api;

import com.restaurant.api.domain.controller.OrderController;
import com.restaurant.api.domain.dto.OrderPaymentMethodUpdateRequest;
import com.restaurant.api.domain.dto.OrderPaymentRequest;
import com.restaurant.api.domain.dto.OrderPaymentStatusResponse;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.OrderType;
import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.enums.StatusPayment;
import com.restaurant.api.domain.repository.*;
import com.restaurant.api.domain.service.DailyOrderSequenceService;
import com.restaurant.api.domain.service.OrderService;
import com.restaurant.api.domain.service.PaymentService;
import com.restaurant.api.domain.service.RestaurantConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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

/**
 * Empirical Adversarial Challenge Suite for Milestone 1 (F1, F2, F3, F4).
 * Developed by challenger_m1_1 to stress-test edge cases, concurrency, batching, and business invariants.
 */
public class AdversarialMilestone1ChallengeTest {

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
  private PaymentService paymentService;
  private OrderController orderController;
  private RestaurantConfig restaurantConfig;

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

    restaurantConfig = new RestaurantConfig();
    restaurantConfig.setDeliveryFee(BigDecimal.valueOf(5.00));
    when(configService.getConfigEntity()).thenReturn(restaurantConfig);
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

    paymentService = new PaymentService(
        paymentRepository, orderRepository, orderItemRepository, tableRepository, configService
    );

    orderController = new OrderController(orderService);
  }

  // =========================================================================
  // SUITE 1: IDEMPOTENCY & CONCURRENCY ADVERSARIAL CHALLENGES (F2)
  // =========================================================================
  @Nested
  @DisplayName("Adversarial Suite: F2 Idempotency & High-Concurrency")
  class IdempotencyStressTests {

    @Test
    @DisplayName("ADV-F2-1: 50 threads concorrentes com mesmo clientOrderId devem resultar em exatamente 1 pedido persistido")
    void testFiftyThreadsConcurrentContention() throws Exception {
      String clientOrderId = "STRESS-50-THREADS-UUID";
      Order canonicalOrder = new Order();
      canonicalOrder.setId(500L);
      canonicalOrder.setClientOrderId(clientOrderId);
      canonicalOrder.setOrderCode("#500");
      canonicalOrder.setStatus(StatusOrder.OPENED);

      when(orderRepository.findById(500L)).thenReturn(Optional.of(canonicalOrder));

      AtomicInteger persistAttempts = new AtomicInteger(0);

      // Thread vencedora persiste; todas as outras 49 tomam colisão de índice único
      when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
        if (persistAttempts.incrementAndGet() == 1) {
          return canonicalOrder;
        }
        throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_orders_client_order_id");
      });

      // findByClientOrderId retorna vazio antes do primeiro persist e canonicalOrder logo após
      when(orderRepository.findByClientOrderId(clientOrderId)).thenAnswer(invocation -> {
        if (persistAttempts.get() > 0) {
          return Optional.of(canonicalOrder);
        }
        return Optional.empty();
      });

      int threadCount = 50;
      ExecutorService pool = Executors.newFixedThreadPool(threadCount);
      CountDownLatch readyLatch = new CountDownLatch(threadCount);
      CountDownLatch startLatch = new CountDownLatch(1);
      List<Future<OrderResponse>> futures = new ArrayList<>();

      OrderRequest request = new OrderRequest(
          null, OrderType.TAKEOUT, null, null, false,
          PaymentMethod.PIX, null, "Observacao concorrente",
          List.of(), null, null, clientOrderId,
          "Cliente Estresse", "38999990000"
      );

      for (int i = 0; i < threadCount; i++) {
        futures.add(pool.submit(() -> {
          readyLatch.countDown();
          startLatch.await(); // Disparo sincronizado em nano-segundos
          return orderService.createOrder(request);
        }));
      }

      readyLatch.await(5, TimeUnit.SECONDS);
      startLatch.countDown();

      for (Future<OrderResponse> future : futures) {
        OrderResponse resp = future.get(10, TimeUnit.SECONDS);
        assertNotNull(resp);
        assertEquals(500L, resp.id());
        assertEquals("#500", resp.orderCode());
        assertEquals(clientOrderId, resp.clientOrderId());
      }

      pool.shutdown();
      assertTrue(persistAttempts.get() >= 1, "Pelo menos 1 tentativa de persistência deve ter ocorrido");
    }

    @Test
    @DisplayName("ADV-F2-2: clientOrderId com espaços ao redor deve colidir e retornar pedido canônico")
    void testWhitespaceNormalizationInClientOrderId() {
      String cleanId = "CANONICAL-UUID-777";
      String untrimmedId = "   CANONICAL-UUID-777   \t";

      Order canonicalOrder = new Order();
      canonicalOrder.setId(777L);
      canonicalOrder.setClientOrderId(cleanId);
      canonicalOrder.setOrderCode("#777");

      when(orderRepository.findByClientOrderId(cleanId)).thenReturn(Optional.of(canonicalOrder));
      when(orderRepository.findById(777L)).thenReturn(Optional.of(canonicalOrder));

      OrderRequest req = new OrderRequest(
          null, OrderType.TAKEOUT, null, null, false,
          PaymentMethod.CASH, null, "Nota",
          List.of(), null, null, untrimmedId,
          "Cliente Trim", null
      );

      OrderResponse res = orderService.createOrder(req);

      assertNotNull(res);
      assertEquals(777L, res.id());
      assertEquals(cleanId, res.clientOrderId());
      // Garante que não tentou salvar novo registro
      verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("ADV-F2-3: Requisições com clientOrderId nulo ou vazio não devem acionar verificação de duplicidade")
    void testNullAndBlankClientOrderIdAllowed() {
      Order createdOrder = new Order();
      createdOrder.setId(901L);
      createdOrder.setOrderCode("#901");
      when(orderRepository.saveAndFlush(any())).thenReturn(createdOrder);
      when(orderRepository.findById(901L)).thenReturn(Optional.of(createdOrder));

      OrderRequest nullReq = new OrderRequest(null, OrderType.TAKEOUT, null, null, false, PaymentMethod.CASH, null, null, List.of(), null, null, null, null, null);
      OrderRequest emptyReq = new OrderRequest(null, OrderType.TAKEOUT, null, null, false, PaymentMethod.CASH, null, null, List.of(), null, null, "   ", null, null);

      OrderResponse res1 = orderService.createOrder(nullReq);
      OrderResponse res2 = orderService.createOrder(emptyReq);

      assertNotNull(res1);
      assertNotNull(res2);
      verify(orderRepository, never()).findByClientOrderId(anyString());
    }

    @Test
    @DisplayName("ADV-F2-4: OrderController com header X-Client-Order-Id propaga idempotência e retorna 200 OK para duplicata")
    void testHeaderClientOrderIdPropagationAndHttpOk() {
      String headerUuid = "HEADER-CLIENT-UUID-10";
      Order existingOrder = new Order();
      existingOrder.setId(10L);
      existingOrder.setClientOrderId(headerUuid);
      existingOrder.setOrderCode("#10");

      when(orderRepository.existsByClientOrderId(headerUuid)).thenReturn(true);
      when(orderRepository.findByClientOrderId(headerUuid)).thenReturn(Optional.of(existingOrder));
      when(orderRepository.findById(10L)).thenReturn(Optional.of(existingOrder));

      OrderRequest requestWithoutBodyClientOrderId = new OrderRequest(
          null, OrderType.TAKEOUT, null, null, false, PaymentMethod.PIX, null, null, List.of(), null, null, null, null, null
      );

      ResponseEntity<OrderResponse> response = orderController.register(headerUuid, requestWithoutBodyClientOrderId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(10L, response.getBody().id());
      assertEquals(headerUuid, response.getBody().clientOrderId());
    }
  }

  // =========================================================================
  // SUITE 2: QUERY BATCHING & N+1 ELIMINATION ADVERSARIAL CHALLENGES (F1)
  // =========================================================================
  @Nested
  @DisplayName("Adversarial Suite: F1 Query Batching & N+1 Invariants")
  class QueryBatchingTests {

    @Test
    @DisplayName("ADV-F1-1: Listagem com 50 pedidos deve realizar exatamente 1 query de itens e 1 query de pagamentos")
    void testBatchFetchWithFiftyOrdersDoesNotTriggerNPlusOne() {
      List<Order> orders = new ArrayList<>();
      List<Long> orderIds = new ArrayList<>();
      List<OrderItem> items = new ArrayList<>();
      List<Payment> payments = new ArrayList<>();

      Product p = new Product();
      p.setId(10L);
      p.setName("Pizza Especial");
      p.setPrice(BigDecimal.valueOf(50.00));

      for (long i = 1; i <= 50; i++) {
        Order o = new Order();
        o.setId(i);
        o.setOrderCode(String.format("#%04d", i));
        o.setStatus(StatusOrder.OPENED);
        o.setOpenedAt(Instant.now());
        orders.add(o);
        orderIds.add(i);

        // Cada pedido tem 2 itens
        OrderItem it1 = new OrderItem();
        it1.setId(i * 10 + 1);
        it1.setOrder(o);
        it1.setProduct(p);
        it1.setUnitPrice(BigDecimal.valueOf(50.00));
        it1.setQuantity(1);
        items.add(it1);

        OrderItem it2 = new OrderItem();
        it2.setId(i * 10 + 2);
        it2.setOrder(o);
        it2.setProduct(p);
        it2.setUnitPrice(BigDecimal.valueOf(50.00));
        it2.setQuantity(1);
        items.add(it2);

        // Pedidos pares têm pagamento aprovado de R$ 100
        if (i % 2 == 0) {
          Payment pay = new Payment();
          pay.setId(i * 100);
          pay.setOrder(o);
          pay.setAmount(BigDecimal.valueOf(100.00));
          pay.setStatus(StatusPayment.APPROVED);
          payments.add(pay);
        }
      }

      Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 50), 50);
      when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
      when(orderItemRepository.findByOrderIdInWithDetails(orderIds)).thenReturn(items);
      when(paymentRepository.findByOrderIdInAndStatus(orderIds, StatusPayment.APPROVED)).thenReturn(payments);

      Page<OrderResponse> resultPage = orderService.findAll(null, null, null, null, PageRequest.of(0, 50));

      assertEquals(50, resultPage.getContent().size());

      // Invariantes estritas de consultas:
      verify(orderItemRepository, times(1)).findByOrderIdInWithDetails(orderIds);
      verify(paymentRepository, times(1)).findByOrderIdInAndStatus(orderIds, StatusPayment.APPROVED);
      verify(configService, times(1)).getConfigEntity();

      // Prova empírica de ausência de chamadas individuais N+1
      verify(orderItemRepository, never()).findByOrderId(anyLong());
      verify(paymentRepository, never()).findByOrderId(anyLong());
      verify(paymentRepository, never()).findByOrderIdAndStatus(anyLong(), any());
      verify(orderRepository, never()).findById(anyLong());

      // Verificação de isolamento de dados entre pedidos mapeados
      for (OrderResponse resp : resultPage.getContent()) {
        assertEquals(2, resp.items().size(), "Cada pedido deve ter exatamente seus 2 itens isolados");
        if (resp.id() % 2 == 0) {
          assertEquals(BigDecimal.valueOf(100.00), resp.totalPaid());
          assertTrue(resp.isFullyPaid());
        } else {
          assertEquals(BigDecimal.ZERO, resp.totalPaid());
          assertFalse(resp.isFullyPaid());
        }
      }
    }

    @Test
    @DisplayName("ADV-F1-2: Página vazia não deve executar consultas de itens ou pagamentos em lote")
    void testEmptyPageBypassesBatchQueries() {
      Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
      when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);

      Page<OrderResponse> result = orderService.findAll(null, null, null, null, PageRequest.of(0, 10));

      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(orderItemRepository, never()).findByOrderIdInWithDetails(any());
      verify(paymentRepository, never()).findByOrderIdInAndStatus(any(), any());
    }
  }

  // =========================================================================
  // SUITE 3: STATUS INVARIANTS — CANCELLED ZEROING & CLOSED SYNC (F3, F4)
  // =========================================================================
  @Nested
  @DisplayName("Adversarial Suite: F3 & F4 Status Invariants")
  class StatusRulesTests {

    @Test
    @DisplayName("ADV-F4-1: Pedido CANCELLED deve zerar compulsoriamente subtotal, taxa, totalAmount, paid e remaining")
    void testCancelledOrderForcesAbsoluteZeroAmounts() {
      Order cancelledOrder = new Order();
      cancelledOrder.setId(88L);
      cancelledOrder.setStatus(StatusOrder.CANCELLED);
      cancelledOrder.setOrderType(OrderType.DELIVERY);

      Product prod = new Product();
      prod.setPrice(BigDecimal.valueOf(90.00));
      OrderItem item = new OrderItem();
      item.setProduct(prod);
      item.setUnitPrice(BigDecimal.valueOf(90.00));
      item.setQuantity(2); // Total nominal de R$ 180,00

      when(orderItemRepository.findByOrderId(88L)).thenReturn(List.of(item));

      // 1. calculateOrderTotal no OrderService
      BigDecimal totalService = orderService.calculateOrderTotal(cancelledOrder);
      assertEquals(BigDecimal.ZERO, totalService, "OrderService.calculateOrderTotal deve ser ZERO");

      // 2. calculateOrderTotal no PaymentService
      BigDecimal totalPaymentService = paymentService.calculateOrderTotal(cancelledOrder);
      assertEquals(BigDecimal.ZERO, totalPaymentService, "PaymentService.calculateOrderTotal deve ser ZERO");

      // 3. mapToOrderResponse
      OrderResponse response = orderService.mapToOrderResponse(cancelledOrder);
      assertEquals(BigDecimal.ZERO, response.subtotal());
      assertEquals(BigDecimal.ZERO, response.deliveryFee());
      assertEquals(BigDecimal.ZERO, response.totalAmount());
      assertEquals(BigDecimal.ZERO, response.totalPaid());
      assertEquals(BigDecimal.ZERO, response.remainingAmount());
      assertFalse(response.isFullyPaid());

      // 4. OrderResponse.fromEntity direto com valores nominais pré-computados
      OrderResponse directResponse = OrderResponse.fromEntity(
          cancelledOrder,
          List.of(),
          BigDecimal.valueOf(180.00),
          BigDecimal.valueOf(5.00),
          BigDecimal.valueOf(185.00),
          BigDecimal.valueOf(185.00),
          BigDecimal.ZERO,
          true
      );
      assertEquals(BigDecimal.ZERO, directResponse.subtotal());
      assertEquals(BigDecimal.ZERO, directResponse.deliveryFee());
      assertEquals(BigDecimal.ZERO, directResponse.totalAmount());
      assertEquals(BigDecimal.ZERO, directResponse.totalPaid());
      assertEquals(BigDecimal.ZERO, directResponse.remainingAmount());
      assertFalse(directResponse.isFullyPaid(), "isFullyPaid deve ser false em pedido CANCELLED");
    }

    @Test
    @DisplayName("ADV-F4-2: updateStatus para CANCELLED deve marcar pagamentos existentes como CANCELLED")
    void testTransitionToCancelledCancelsPaymentsDefensively() {
      Order order = new Order();
      order.setId(303L);
      order.setStatus(StatusOrder.OPENED);

      Payment p1 = new Payment();
      p1.setId(1L);
      p1.setStatus(StatusPayment.APPROVED);
      Payment p2 = new Payment();
      p2.setId(2L);
      p2.setStatus(StatusPayment.APPROVED);

      when(orderRepository.findById(303L)).thenReturn(Optional.of(order));
      when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
      when(paymentRepository.findByOrderId(303L)).thenReturn(List.of(p1, p2));

      OrderResponse resp = orderService.updateStatus(303L, StatusOrder.CANCELLED);

      assertEquals(StatusOrder.CANCELLED, resp.status());
      assertEquals(StatusPayment.CANCELLED, p1.getStatus());
      assertEquals(StatusPayment.CANCELLED, p2.getStatus());
      verify(paymentRepository).saveAll(List.of(p1, p2));
    }

    @Test
    @DisplayName("ADV-F3-1: Fechamento (CLOSED) sem pagamentos gera registro APPROVED com total do pedido e forma coerente")
    void testTransitionToClosedGeneratesApprovedPayment() {
      Order order = new Order();
      order.setId(404L);
      order.setStatus(StatusOrder.DELIVERED);
      order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
      order.setCustomerName("Maria Fechamento");

      Product prod = new Product();
      prod.setPrice(BigDecimal.valueOf(75.00));
      OrderItem it = new OrderItem();
      it.setProduct(prod);
      it.setUnitPrice(BigDecimal.valueOf(75.00));
      it.setQuantity(1);

      when(orderRepository.findById(404L)).thenReturn(Optional.of(order));
      when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
      when(paymentRepository.findByOrderId(404L)).thenReturn(Collections.emptyList());
      when(orderItemRepository.findByOrderId(404L)).thenReturn(List.of(it));

      orderService.updateStatus(404L, StatusOrder.CLOSED);

      ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository).save(paymentCaptor.capture());

      Payment savedPayment = paymentCaptor.getValue();
      assertNotNull(savedPayment);
      assertEquals(BigDecimal.valueOf(75.00), savedPayment.getAmount());
      assertEquals(PaymentMethod.CREDIT_CARD, savedPayment.getMethod());
      assertEquals(StatusPayment.APPROVED, savedPayment.getStatus());
      assertEquals("Maria Fechamento", savedPayment.getPayerName());
    }

    @Test
    @DisplayName("ADV-F3-2: Fechamento (CLOSED) com pagamento prévio de método divergente sincroniza método na tabela payment")
    void testTransitionToClosedSynchronizesPaymentMethodDivergence() {
      Order order = new Order();
      order.setId(505L);
      order.setStatus(StatusOrder.DELIVERED);
      order.setPaymentMethod(PaymentMethod.PIX); // Pedido marcado como PIX

      Payment singlePay = new Payment();
      singlePay.setId(10L);
      singlePay.setMethod(PaymentMethod.CASH); // Gravado inicialmente como DINHEIRO
      singlePay.setStatus(StatusPayment.APPROVED);
      singlePay.setAmount(BigDecimal.valueOf(50.00));

      when(orderRepository.findById(505L)).thenReturn(Optional.of(order));
      when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
      when(paymentRepository.findByOrderId(505L)).thenReturn(List.of(singlePay));

      orderService.updateStatus(505L, StatusOrder.CLOSED);

      assertEquals(PaymentMethod.PIX, singlePay.getMethod(), "Método no pagamento deve ser sincronizado com o pedido");
      verify(paymentRepository).save(singlePay);
    }

    @Test
    @DisplayName("ADV-F3-3: PaymentService.registerPayment quitando o pedido atualiza pedido para CLOSED e sincroniza paymentMethod")
    void testRegisterPaymentFullyPayingClosesOrderAndCascades() {
      Order order = new Order();
      order.setId(606L);
      order.setStatus(StatusOrder.OPENED);
      order.setOrderType(OrderType.TAKEOUT);
      order.setPaymentMethod(null);

      OrderItem item = new OrderItem();
      item.setId(1L);
      item.setStatus(StatusOrderItem.PENDING);
      item.setUnitPrice(BigDecimal.valueOf(45.00));
      item.setQuantity(1);

      List<Payment> savedPayments = new ArrayList<>();
      when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
        Payment p = inv.getArgument(0);
        savedPayments.add(p);
        return p;
      });
      when(orderRepository.findById(606L)).thenReturn(Optional.of(order));
      when(orderItemRepository.findByOrderId(606L)).thenReturn(List.of(item));
      when(paymentRepository.findByOrderId(606L)).thenAnswer(inv -> savedPayments);
      when(paymentRepository.findByOrderIdAndStatus(606L, StatusPayment.APPROVED)).thenReturn(Collections.emptyList());

      OrderPaymentRequest payReq = new OrderPaymentRequest(PaymentMethod.DEBIT_CARD, BigDecimal.valueOf(45.00), "Cliente Quitando");

      OrderPaymentStatusResponse statusResp = paymentService.registerPayment(606L, payReq);

      assertNotNull(statusResp);
      assertTrue(statusResp.isFullyPaid());
      assertEquals(0, statusResp.remainingAmount().compareTo(BigDecimal.ZERO));
      assertEquals(StatusOrder.CLOSED, order.getStatus(), "Pedido deve transicionar para CLOSED ao quitar integralmente");
      assertEquals(PaymentMethod.DEBIT_CARD, order.getPaymentMethod(), "order.paymentMethod deve ser preenchido com a forma quitada");
      assertEquals(StatusOrderItem.DELIVERED, item.getStatus(), "Itens pendentes devem cascatear para DELIVERED");
      verify(orderRepository).save(order);
      verify(orderItemRepository).saveAll(List.of(item));
    }
  }
}
