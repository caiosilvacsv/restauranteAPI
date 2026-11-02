package com.restaurant.api;

import com.restaurant.api.domain.dto.*;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.*;
import com.restaurant.api.domain.repository.*;
import com.restaurant.api.domain.service.DailyOrderSequenceService;
import com.restaurant.api.domain.service.OrderService;
import com.restaurant.api.domain.service.PaymentService;
import com.restaurant.api.domain.service.RestaurantConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

@SpringBootTest
public class Milestone1AdversarialChallengeTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private PaymentService paymentService;

  // =========================================================================
  // CHALLENGE 1: Real Database Concurrency & Idempotency Stress Test
  // =========================================================================

  @Test
  @DisplayName("Adversarial C1.1: 20 threads concorrentes no PostgreSQL com mesmo clientOrderId geram exatamente 1 pedido")
  void testRealDatabase20ConcurrentThreadsSameClientOrderId() throws Exception {
    String clientOrderId = "ADV-RACE-" + UUID.randomUUID();
    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startGate = new CountDownLatch(1);
    List<Future<OrderResponse>> futures = new ArrayList<>();

    OrderRequest request = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.PIX, null, "Stress test conc",
        List.of(), null, null, clientOrderId,
        "Cliente Concorrente", "38999990001"
    );

    for (int i = 0; i < threadCount; i++) {
      futures.add(executor.submit(() -> {
        startGate.await();
        return orderService.createOrder(request);
      }));
    }

    // Libera as 20 threads simultaneamente contra o PostgreSQL
    startGate.countDown();

    Set<Long> returnedOrderIds = new HashSet<>();
    for (Future<OrderResponse> f : futures) {
      OrderResponse res = f.get(10, TimeUnit.SECONDS);
      assertNotNull(res);
      assertNotNull(res.id());
      assertEquals(clientOrderId, res.clientOrderId());
      returnedOrderIds.add(res.id());
    }

    executor.shutdown();

    // Todas as 20 threads devem receber o mesmo ID de pedido
    assertEquals(1, returnedOrderIds.size(), "Todas as threads concorrentes devem retornar o mesmo pedido");

    Long persistedId = returnedOrderIds.iterator().next();
    Optional<Order> inDb = orderRepository.findByClientOrderId(clientOrderId);
    assertTrue(inDb.isPresent(), "O pedido deve existir no banco");
    assertEquals(persistedId, inDb.get().getId());
  }

  @Test
  @DisplayName("Adversarial C1.2: Sanitização de espaços em clientOrderId mantém unicidade no banco")
  void testClientOrderIdWhitespaceEquivalenceInRealDb() {
    String rawUuid = "ADV-WS-" + UUID.randomUUID();
    String spacedUuid = "   " + rawUuid + "   \t";

    OrderRequest req1 = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "Com espacos",
        List.of(), null, null, spacedUuid,
        "Cliente WS 1", "38999990002"
    );

    OrderResponse res1 = orderService.createOrder(req1);
    assertNotNull(res1);
    assertEquals(rawUuid, res1.clientOrderId());

    OrderRequest req2 = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "Sem espacos",
        List.of(), null, null, rawUuid,
        "Cliente WS 2", "38999990002"
    );

    OrderResponse res2 = orderService.createOrder(req2);
    assertNotNull(res2);
    assertEquals(res1.id(), res2.id(), "Requisições com e sem espaços no clientOrderId devem resolver para o mesmo pedido");
  }

  @Test
  @DisplayName("Adversarial C1.3: clientOrderId nulo ou vazio permite múltiplos pedidos sem colisão")
  void testNullAndBlankClientOrderIdDoNotCollide() {
    OrderRequest reqNull = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "Sem clientOrderId",
        List.of(), null, null, null,
        "Cliente Nulo", "38999990003"
    );

    OrderRequest reqBlank = new OrderRequest(
        null, OrderType.TAKEOUT, null, null, false,
        PaymentMethod.CASH, null, "clientOrderId em branco",
        List.of(), null, null, "   ",
        "Cliente Branco", "38999990004"
    );

    OrderResponse res1 = orderService.createOrder(reqNull);
    OrderResponse res2 = orderService.createOrder(reqBlank);

    assertNotNull(res1);
    assertNotNull(res2);
    assertNotEquals(res1.id(), res2.id(), "Pedidos sem clientOrderId não devem colidir");
    assertNull(res1.clientOrderId());
    assertNull(res2.clientOrderId());
  }

  // =========================================================================
  // CHALLENGE 2: Query Batching & Elimination of N+1 on OrderService.findAll
  // =========================================================================

  @Test
  @DisplayName("Adversarial C2.1: findAll com 5 pedidos deve executar apenas 1 batch de itens e 1 batch de pagamentos (0 chamadas individuais)")
  void testFindAllBatchingDeterministicMockVerification() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    RestaurantTableRepository mockTableRepo = mock(RestaurantTableRepository.class);
    ProductRepository mockProductRepo = mock(ProductRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    IngredientRepository mockIngRepo = mock(IngredientRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);
    PizzaSizeRepository mockSizeRepo = mock(PizzaSizeRepository.class);
    ProductSizePriceRepository mockPspRepo = mock(ProductSizePriceRepository.class);
    UserAddressRepository mockAddrRepo = mock(UserAddressRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    DailyOrderSequenceService mockSeqService = mock(DailyOrderSequenceService.class);
    UserRepository mockUserRepo = mock(UserRepository.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(3.00));
    when(mockConfigService.getConfigEntity()).thenReturn(config);

    List<Order> orders = new ArrayList<>();
    List<Long> orderIds = List.of(501L, 502L, 503L, 504L, 505L);
    for (Long id : orderIds) {
      Order o = new Order();
      o.setId(id);
      o.setStatus(StatusOrder.OPENED);
      o.setOrderType(OrderType.TAKEOUT);
      o.setOpenedAt(Instant.now());
      orders.add(o);
    }

    Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), 5);
    when(mockOrderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    when(mockItemRepo.findByOrderIdInWithDetails(orderIds)).thenReturn(Collections.emptyList());
    when(mockPaymentRepo.findByOrderIdInAndStatus(orderIds, StatusPayment.APPROVED)).thenReturn(Collections.emptyList());

    OrderService isolatedService = new OrderService(
        mockOrderRepo, mockTableRepo, mockProductRepo, mockItemRepo, mockIngRepo,
        mockConfigService, mockSizeRepo, mockPspRepo, mockAddrRepo, mockPaymentRepo,
        mockSeqService, mockUserRepo
    );

    Page<OrderResponse> result = isolatedService.findAll(null, null, null, null, PageRequest.of(0, 10));

    assertEquals(5, result.getContent().size());

    // Verificações estritas:
    verify(mockItemRepo, times(1)).findByOrderIdInWithDetails(orderIds);
    verify(mockPaymentRepo, times(1)).findByOrderIdInAndStatus(orderIds, StatusPayment.APPROVED);
    verify(mockConfigService, times(1)).getConfigEntity();

    // Verificação de que N+1 NUNCA ocorreu:
    verify(mockItemRepo, never()).findByOrderId(anyLong());
    verify(mockPaymentRepo, never()).findByOrderIdAndStatus(anyLong(), any());
    verify(mockOrderRepo, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Adversarial C2.2: findAll com página vazia lida graciosamente sem invocar repositórios desnecessários")
  void testFindAllWithEmptyPage() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);

    when(mockOrderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

    OrderService isolatedService = new OrderService(
        mockOrderRepo, null, null, mockItemRepo, null,
        mockConfigService, null, null, null, mockPaymentRepo,
        null, null
    );

    Page<OrderResponse> result = isolatedService.findAll(null, null, null, null, PageRequest.of(0, 10));

    assertTrue(result.isEmpty());
    verify(mockItemRepo, never()).findByOrderIdInWithDetails(any());
    verify(mockPaymentRepo, never()).findByOrderIdInAndStatus(any(), any());
  }

  @Test
  @DisplayName("Adversarial C2.3: findAll agrupa corretamente itens e pagamentos distribuídos entre diferentes pedidos")
  void testFindAllGroupsItemsAndPaymentsCorrectly() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(2.00));
    when(mockConfigService.getConfigEntity()).thenReturn(config);

    Order o1 = new Order(); o1.setId(601L); o1.setStatus(StatusOrder.OPENED); o1.setOrderType(OrderType.TAKEOUT);
    Order o2 = new Order(); o2.setId(602L); o2.setStatus(StatusOrder.DELIVERED); o2.setOrderType(OrderType.DELIVERY);

    when(mockOrderRepo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(o1, o2)));

    Product p = new Product(); p.setId(1L); p.setName("Pizza"); p.setPrice(BigDecimal.valueOf(50.00));

    OrderItem it1 = new OrderItem(); it1.setId(10L); it1.setOrder(o1); it1.setProduct(p); it1.setQuantity(1); it1.setUnitPrice(BigDecimal.valueOf(50.00));
    OrderItem it2 = new OrderItem(); it2.setId(20L); it2.setOrder(o2); it2.setProduct(p); it2.setQuantity(2); it2.setUnitPrice(BigDecimal.valueOf(50.00));

    when(mockItemRepo.findByOrderIdInWithDetails(List.of(601L, 602L))).thenReturn(List.of(it1, it2));

    Payment pay2 = new Payment(); pay2.setId(30L); pay2.setOrder(o2); pay2.setAmount(BigDecimal.valueOf(102.00)); pay2.setStatus(StatusPayment.APPROVED);
    when(mockPaymentRepo.findByOrderIdInAndStatus(List.of(601L, 602L), StatusPayment.APPROVED)).thenReturn(List.of(pay2));

    OrderService isolatedService = new OrderService(
        mockOrderRepo, null, null, mockItemRepo, null,
        mockConfigService, null, null, null, mockPaymentRepo,
        null, null
    );

    Page<OrderResponse> result = isolatedService.findAll(null, null, null, null, PageRequest.of(0, 10));

    assertEquals(2, result.getContent().size());

    OrderResponse res1 = result.getContent().get(0);
    assertEquals(601L, res1.id());
    assertEquals(0, new BigDecimal("50.00").compareTo(res1.subtotal()));
    assertEquals(0, BigDecimal.ZERO.compareTo(res1.deliveryFee()));
    assertEquals(0, new BigDecimal("50.00").compareTo(res1.totalAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(res1.totalPaid()));
    assertEquals(0, new BigDecimal("50.00").compareTo(res1.remainingAmount()));
    assertFalse(res1.isFullyPaid());

    OrderResponse res2 = result.getContent().get(1);
    assertEquals(602L, res2.id());
    assertEquals(0, new BigDecimal("100.00").compareTo(res2.subtotal()));
    assertEquals(0, new BigDecimal("2.00").compareTo(res2.deliveryFee()));
    assertEquals(0, new BigDecimal("102.00").compareTo(res2.totalAmount()));
    assertEquals(0, new BigDecimal("102.00").compareTo(res2.totalPaid()));
    assertEquals(0, BigDecimal.ZERO.compareTo(res2.remainingAmount()));
    assertTrue(res2.isFullyPaid());
  }

  // =========================================================================
  // CHALLENGE 3: Status Transitions - CANCELLED Zeroing & CLOSED Payments
  // =========================================================================

  @Test
  @DisplayName("Adversarial C3.1: Cancelamento via updateStatus(CANCELLED) zera valores e cancela pagamentos aprovados")
  void testUpdateStatusToCancelledZerosOutAndCancelsPayments() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(5.00));
    when(mockConfigService.getConfigEntity()).thenReturn(config);

    Order order = new Order();
    order.setId(701L);
    order.setStatus(StatusOrder.OPENED);
    order.setOrderType(OrderType.DELIVERY);

    Payment payment = new Payment();
    payment.setId(801L);
    payment.setOrder(order);
    payment.setAmount(BigDecimal.valueOf(50.00));
    payment.setStatus(StatusPayment.APPROVED);

    when(mockOrderRepo.findById(701L)).thenReturn(Optional.of(order));
    when(mockOrderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    when(mockPaymentRepo.findByOrderId(701L)).thenReturn(List.of(payment));
    when(mockItemRepo.findByOrderId(701L)).thenReturn(Collections.emptyList());

    OrderService isolatedService = new OrderService(
        mockOrderRepo, null, null, mockItemRepo, null,
        mockConfigService, null, null, null, mockPaymentRepo,
        null, null
    );

    OrderResponse response = isolatedService.updateStatus(701L, StatusOrder.CANCELLED);

    assertEquals(StatusOrder.CANCELLED, response.status());
    assertEquals(BigDecimal.ZERO, response.subtotal());
    assertEquals(BigDecimal.ZERO, response.deliveryFee());
    assertEquals(BigDecimal.ZERO, response.totalAmount());
    assertEquals(BigDecimal.ZERO, response.totalPaid());
    assertEquals(BigDecimal.ZERO, response.remainingAmount());
    assertFalse(response.isFullyPaid());

    // Pagamento deve ter sido cancelado compulsoriamente
    assertEquals(StatusPayment.CANCELLED, payment.getStatus());
    verify(mockPaymentRepo, times(1)).saveAll(List.of(payment));
  }

  @Test
  @DisplayName("Adversarial C3.2: Fechamento via updateStatus(CLOSED) gera pagamento aprovado com valor total se nenhum pagamento existia")
  void testUpdateStatusToClosedGeneratesPaymentIfNoneExisted() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(BigDecimal.valueOf(4.00));
    when(mockConfigService.getConfigEntity()).thenReturn(config);

    Order order = new Order();
    order.setId(702L);
    order.setStatus(StatusOrder.OPENED);
    order.setOrderType(OrderType.DELIVERY);
    order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
    order.setCustomerName("Maria Fechamento");

    Product p = new Product(); p.setId(2L); p.setPrice(BigDecimal.valueOf(46.00));
    OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(p); item.setQuantity(1); item.setUnitPrice(BigDecimal.valueOf(46.00));

    when(mockOrderRepo.findById(702L)).thenReturn(Optional.of(order));
    when(mockOrderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    when(mockPaymentRepo.findByOrderId(702L)).thenReturn(Collections.emptyList());
    when(mockItemRepo.findByOrderId(702L)).thenReturn(List.of(item));

    List<Payment> savedPayments = new ArrayList<>();
    when(mockPaymentRepo.save(any(Payment.class))).thenAnswer(i -> {
      Payment saved = i.getArgument(0);
      savedPayments.add(saved);
      return saved;
    });

    when(mockPaymentRepo.findByOrderIdAndStatus(702L, StatusPayment.APPROVED)).thenAnswer(i -> savedPayments);

    OrderService isolatedService = new OrderService(
        mockOrderRepo, null, null, mockItemRepo, null,
        mockConfigService, null, null, null, mockPaymentRepo,
        null, null
    );

    OrderResponse response = isolatedService.updateStatus(702L, StatusOrder.CLOSED);

    assertEquals(StatusOrder.CLOSED, response.status());
    // Total: 46 (item) + 4 (delivery) = 50.00
    assertEquals(0, new BigDecimal("50.00").compareTo(response.totalAmount()));
    assertEquals(1, savedPayments.size(), "Deve ter salvo exatamente 1 pagamento de fechamento");
    Payment createdPay = savedPayments.get(0);
    assertEquals(0, new BigDecimal("50.00").compareTo(createdPay.getAmount()));
    assertEquals(PaymentMethod.CREDIT_CARD, createdPay.getMethod());
    assertEquals(StatusPayment.APPROVED, createdPay.getStatus());
    assertEquals("Maria Fechamento", createdPay.getPayerName());
    assertTrue(response.isFullyPaid());
  }

  @Test
  @DisplayName("Adversarial C3.3: Fechamento via updateStatus(CLOSED) sincroniza método de pagamento divergente")
  void testUpdateStatusToClosedSyncsMismatchedPaymentMethod() {
    OrderRepository mockOrderRepo = mock(OrderRepository.class);
    PaymentRepository mockPaymentRepo = mock(PaymentRepository.class);
    OrderItemRepository mockItemRepo = mock(OrderItemRepository.class);
    RestaurantConfigService mockConfigService = mock(RestaurantConfigService.class);

    RestaurantConfig config = new RestaurantConfig();
    when(mockConfigService.getConfigEntity()).thenReturn(config);

    Order order = new Order();
    order.setId(703L);
    order.setStatus(StatusOrder.OPENED);
    order.setOrderType(OrderType.TAKEOUT);
    order.setPaymentMethod(PaymentMethod.PIX); // Método pretendido mudou para PIX

    Payment existingPay = new Payment();
    existingPay.setId(901L);
    existingPay.setOrder(order);
    existingPay.setAmount(BigDecimal.valueOf(30.00));
    existingPay.setMethod(PaymentMethod.CASH); // Pagamento prévio constava como CASH
    existingPay.setStatus(StatusPayment.APPROVED);

    when(mockOrderRepo.findById(703L)).thenReturn(Optional.of(order));
    when(mockOrderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    when(mockPaymentRepo.findByOrderId(703L)).thenReturn(List.of(existingPay));
    when(mockItemRepo.findByOrderId(703L)).thenReturn(Collections.emptyList());
    when(mockPaymentRepo.findByOrderIdAndStatus(703L, StatusPayment.APPROVED)).thenReturn(List.of(existingPay));

    OrderService isolatedService = new OrderService(
        mockOrderRepo, null, null, mockItemRepo, null,
        mockConfigService, null, null, null, mockPaymentRepo,
        null, null
    );

    isolatedService.updateStatus(703L, StatusOrder.CLOSED);

    assertEquals(PaymentMethod.PIX, existingPay.getMethod(), "O pagamento existente deve ser sincronizado com o método do pedido");
    verify(mockPaymentRepo, times(1)).save(existingPay);
  }
}
