package com.restaurant.api;

import com.restaurant.api.domain.dto.DeliveryAddressDto;
import com.restaurant.api.domain.entity.DeliveryAddress;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.entity.Product;
import com.restaurant.api.domain.entity.RestaurantConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class HibridoKdsPizzariaTest {

  @Test
  @DisplayName("Deve calcular preço de pizza meio-a-meio arredondando sempre para cima no próximo inteiro (CEILING)")
  void testHalfAndHalfArithmeticMeanPrice() {
    Product flavor1 = new Product();
    flavor1.setName("Calabresa");
    flavor1.setPrice(BigDecimal.valueOf(40.00));
    flavor1.setAllowsHalfAndHalf(true);

    Product flavor2 = new Product();
    flavor2.setName("Frango c/ Catupiry");
    flavor2.setPrice(BigDecimal.valueOf(45.00));
    flavor2.setAllowsHalfAndHalf(true);

    OrderItem item = new OrderItem();
    item.setProduct(flavor1);
    item.setSecondProduct(flavor2);

    assertTrue(item.isHalfAndHalf());

    // Média de 40.00 e 45.00 = 42.50 -> arredonda para cima = 43.00
    BigDecimal calculated = item.calculateUnitPrice();
    assertEquals(new BigDecimal("43.00"), calculated);

    // Teste com valor quebrado que vira inteiro: 10.01 -> 11.00
    flavor1.setPrice(new BigDecimal("10.01"));
    flavor2.setPrice(new BigDecimal("10.01"));
    assertEquals(new BigDecimal("11.00"), item.calculateUnitPrice());
  }

  @Test
  @DisplayName("Deve retornar preço integral caso a pizza seja de 1 sabor só")
  void testSingleFlavorPrice() {
    Product flavor = new Product();
    flavor.setName("Calabresa");
    flavor.setPrice(BigDecimal.valueOf(40.00));

    OrderItem item = new OrderItem();
    item.setProduct(flavor);
    item.setSecondProduct(null);

    assertFalse(item.isHalfAndHalf());
    assertEquals(new BigDecimal("40.00"), item.calculateUnitPrice());
  }

  @Test
  @DisplayName("Deve priorizar o valor do tamanho da pizza quando o tamanho possuir preço definido")
  void testPizzaSizePriceUsageInOrderItem() {
    Product flavor = new Product();
    flavor.setName("Calabresa");
    flavor.setPrice(BigDecimal.valueOf(40.00));

    com.restaurant.api.domain.entity.PizzaSize size = new com.restaurant.api.domain.entity.PizzaSize();
    size.setName("Pizza Grande");
    size.setShortName("G");
    size.setPrice(new BigDecimal("55.00"));

    OrderItem item = new OrderItem();
    item.setProduct(flavor);
    item.setPizzaSize(size);

    // O preço deve ser o do tamanho (55.00) e não o do sabor base (40.00)
    assertEquals(new BigDecimal("55.00"), item.calculateUnitPrice());

    // Se o tamanho não tiver preço ou for 0, deve usar o preço do produto (40.00)
    size.setPrice(BigDecimal.ZERO);
    assertEquals(new BigDecimal("40.00"), item.calculateUnitPrice());
  }

  @Test
  @DisplayName("Deve indicar restaurante fechado quando fechamento manual emergencial estiver ativado")
  void testManuallyClosedStore() {
    RestaurantConfig config = new RestaurantConfig();
    config.setOpenTime(LocalTime.of(0, 0));
    config.setCloseTime(LocalTime.of(23, 59));
    config.setManuallyClosed(true);
    config.setClosedMessage("Fechado para manutenção!");

    assertFalse(config.isCurrentlyOpen());
    assertEquals("Fechado para manutenção!", config.getClosedMessage());
  }

  @Test
  @DisplayName("Deve mapear corretamente dados de endereço de entrega para Taiobeiras - MG")
  void testDeliveryAddressMapping() {
    DeliveryAddressDto dto = new DeliveryAddressDto(
        "Av. do Contorno",
        "100",
        "Centro",
        "Apto 201",
        "Próximo à Praça"
    );

    DeliveryAddress entity = dto.toEntity();
    assertEquals("Av. do Contorno", entity.getStreet());
    assertEquals("100", entity.getNumber());
    assertEquals("Centro", entity.getNeighborhood());
    assertEquals("Apto 201", entity.getComplement());
    assertEquals("Próximo à Praça", entity.getReferencePoint());

    DeliveryAddressDto mappedBack = DeliveryAddressDto.fromEntity(entity);
    assertEquals(dto.street(), mappedBack.street());
    assertEquals(dto.number(), mappedBack.number());
    assertEquals(dto.neighborhood(), mappedBack.neighborhood());
  }

  @Test
  @DisplayName("Deve validar atributos de PizzaSize e nome curto para comanda da cozinha")
  void testPizzaSizeAttributesAndShortName() {
    com.restaurant.api.domain.entity.PizzaSize size = new com.restaurant.api.domain.entity.PizzaSize();
    size.setName("Grande");
    size.setShortName("G");
    size.setSlices(8);
    size.setMaxFlavors(2);
    size.setActive(true);

    assertEquals("Grande", size.getName());
    assertEquals("G", size.getShortName());
    assertEquals(8, size.getSlices());
    assertEquals(2, size.getMaxFlavors());
    assertTrue(size.getActive());
  }

  @Test
  @DisplayName("Deve validar papel WAITER e mapeamento de mesa com pedido ativo")
  void testWaiterRoleAndTableMapping() {
    com.restaurant.api.domain.entity.User waiter = new com.restaurant.api.domain.entity.User();
    waiter.setName("Carlos Garçom");
    waiter.setRole(com.restaurant.api.domain.enums.Role.WAITER);

    assertTrue(waiter.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WAITER")));
    assertTrue(waiter.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

    com.restaurant.api.domain.entity.RestaurantTables table = new com.restaurant.api.domain.entity.RestaurantTables();
    table.setId(1L);
    table.setNumber(5);
    table.setCapacity(4);
    table.setStatus(com.restaurant.api.domain.enums.StatusTable.OCCUPIED);

    com.restaurant.api.domain.dto.TableResponse response = com.restaurant.api.domain.dto.TableResponse.fromEntity(
        table, 102L, "#0102", java.time.Instant.now()
    );

    assertEquals(5, response.number());
    assertEquals(com.restaurant.api.domain.enums.StatusTable.OCCUPIED, response.status());
    assertEquals(102L, response.currentOrderId());
    assertEquals("#0102", response.currentOrderCode());
  }

  @Test
  @DisplayName("Deve conter campos resumidos de pagamento totalPaid, remainingAmount e isFullyPaid no OrderResponse")
  void testOrderResponsePaymentSummary() {
    com.restaurant.api.domain.entity.Order order = new com.restaurant.api.domain.entity.Order();
    order.setId(10L);
    order.setOrderCode("#0010");
    order.setStatus(com.restaurant.api.domain.enums.StatusOrder.OPENED);

    BigDecimal subtotal = new BigDecimal("45.00");
    BigDecimal deliveryFee = new BigDecimal("2.00");
    BigDecimal totalAmount = new BigDecimal("47.00");
    BigDecimal totalPaid = new BigDecimal("20.00");
    BigDecimal remainingAmount = new BigDecimal("27.00");
    Boolean isFullyPaid = false;

    com.restaurant.api.domain.dto.OrderResponse response = com.restaurant.api.domain.dto.OrderResponse.fromEntity(
        order,
        java.util.List.of(),
        subtotal,
        deliveryFee,
        totalAmount,
        totalPaid,
        remainingAmount,
        isFullyPaid
    );

    assertEquals(new BigDecimal("47.00"), response.totalAmount());
    assertEquals(new BigDecimal("20.00"), response.totalPaid());
    assertEquals(new BigDecimal("27.00"), response.remainingAmount());
    assertFalse(response.isFullyPaid());
  }

  @Test
  @DisplayName("Deve testar idempotência do KitchenService para itens na cozinha")
  void testKitchenServiceIdempotency() {
    com.restaurant.api.domain.repository.OrderItemRepository mockRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.OrderItemRepository.class);
    com.restaurant.api.domain.service.KitchenService kitchenService = new com.restaurant.api.domain.service.KitchenService(mockRepo);

    OrderItem item = new OrderItem();
    item.setId(1L);
    item.setStatus(com.restaurant.api.domain.enums.StatusOrderItem.READY);

    org.mockito.Mockito.when(mockRepo.findById(1L)).thenReturn(java.util.Optional.of(item));

    // Item já está READY. Chamar initPreparing (etapa anterior) deve retornar o item sem erro (idempotente)
    com.restaurant.api.domain.dto.KitchenItemResponse respPrep = kitchenService.initPreparing(1L);
    assertEquals(com.restaurant.api.domain.enums.StatusOrderItem.READY, respPrep.status());

    // Chamar markReady (mesma etapa) também deve retornar o item sem erro
    com.restaurant.api.domain.dto.KitchenItemResponse respReady = kitchenService.markReady(1L);
    assertEquals(com.restaurant.api.domain.enums.StatusOrderItem.READY, respReady.status());

    // Nenhuma gravação desnecessária deve ter ocorrido já que o item já estava em estágio mais avançado
    org.mockito.Mockito.verify(mockRepo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Deve validar atribuição correta de status em OrderItem.next()")
  void testOrderItemNext() {
    OrderItem item = new OrderItem();
    item.setStatus(com.restaurant.api.domain.enums.StatusOrderItem.PENDING);

    item.next();
    assertEquals(com.restaurant.api.domain.enums.StatusOrderItem.QUEUED, item.getStatus());

    item.next();
    assertEquals(com.restaurant.api.domain.enums.StatusOrderItem.PREPARING, item.getStatus());
  }

  @Test
  @DisplayName("Deve inicializar admin padrão via AdminInitializer se usuário não existir")
  void testAdminInitializerBootstrapsAdmin() {
    com.restaurant.api.domain.repository.UserRepository mockUserRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.UserRepository.class);
    org.springframework.security.crypto.password.PasswordEncoder mockEncoder = org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);

    org.mockito.Mockito.when(mockUserRepo.existsByLogin("admin")).thenReturn(false);
    org.mockito.Mockito.when(mockEncoder.encode("secret123")).thenReturn("$2a$10$encodedHash");

    com.restaurant.api.domain.infra.security.AdminInitializer initializer =
        new com.restaurant.api.domain.infra.security.AdminInitializer(mockUserRepo, mockEncoder);

    // Injeta campos via reflection
    org.springframework.test.util.ReflectionTestUtils.setField(initializer, "adminLogin", "admin");
    org.springframework.test.util.ReflectionTestUtils.setField(initializer, "adminPassword", "secret123");
    org.springframework.test.util.ReflectionTestUtils.setField(initializer, "adminName", "Super Admin");
    org.springframework.test.util.ReflectionTestUtils.setField(initializer, "adminPhone", "5511999999999");

    initializer.run();

    org.mockito.ArgumentCaptor<com.restaurant.api.domain.entity.User> captor =
        org.mockito.ArgumentCaptor.forClass(com.restaurant.api.domain.entity.User.class);
    org.mockito.Mockito.verify(mockUserRepo, org.mockito.Mockito.times(1)).save(captor.capture());

    com.restaurant.api.domain.entity.User saved = captor.getValue();
    assertEquals("admin", saved.getLogin());
    assertEquals("$2a$10$encodedHash", saved.getPassword());
    assertEquals(com.restaurant.api.domain.enums.Role.ADMIN, saved.getRole());
    assertTrue(saved.getActive());
  }

  @Test
  @DisplayName("Deve permitir ADMIN cadastrar novos garçons e administradores via UserService")
  void testCreateStaffSuccess() {
    com.restaurant.api.domain.repository.UserRepository mockUserRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.UserRepository.class);
    org.springframework.security.crypto.password.PasswordEncoder mockEncoder = org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);

    com.restaurant.api.domain.service.UserService userService =
        new com.restaurant.api.domain.service.UserService(mockUserRepo, mockEncoder);

    org.mockito.Mockito.when(mockUserRepo.existsByLogin("garcom_joao")).thenReturn(false);
    org.mockito.Mockito.when(mockEncoder.encode("senhaGarcom123")).thenReturn("$2a$10$hashedGarcom");

    org.mockito.Mockito.when(mockUserRepo.save(org.mockito.ArgumentMatchers.any(com.restaurant.api.domain.entity.User.class)))
        .thenAnswer(invocation -> {
          com.restaurant.api.domain.entity.User u = invocation.getArgument(0);
          u.setId(10L);
          return u;
        });

    com.restaurant.api.domain.dto.StaffRegisterRequest request =
        new com.restaurant.api.domain.dto.StaffRegisterRequest(
            "garcom_joao",
            "senhaGarcom123",
            "João Garçom",
            "5538988887777",
            com.restaurant.api.domain.enums.Role.WAITER
        );

    com.restaurant.api.domain.dto.UserResponse response = userService.createStaff(request);

    assertNotNull(response);
    assertEquals("garcom_joao", response.login());
    assertEquals("WAITER", response.role());
    assertEquals("João Garçom", response.name());
    assertTrue(response.active());
  }

  @Test
  @DisplayName("Deve rejeitar cadastro de equipe com cargos inválidos como CUSTOMER ou USER")
  void testCreateStaffRejectsInvalidRole() {
    com.restaurant.api.domain.repository.UserRepository mockUserRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.UserRepository.class);
    org.springframework.security.crypto.password.PasswordEncoder mockEncoder = org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);

    com.restaurant.api.domain.service.UserService userService =
        new com.restaurant.api.domain.service.UserService(mockUserRepo, mockEncoder);

    com.restaurant.api.domain.dto.StaffRegisterRequest request =
        new com.restaurant.api.domain.dto.StaffRegisterRequest(
            "cliente1",
            "senha123",
            "Cliente Teste",
            "5538988887777",
            com.restaurant.api.domain.enums.Role.CUSTOMER
        );

    assertThrows(com.restaurant.api.domain.exception.BusinessRulesException.class, () -> {
      userService.createStaff(request);
    });
  }

  @Test
  @DisplayName("Deve validar dias de funcionamento em RestaurantConfig")
  void testOpenDaysLogic() {
    RestaurantConfig config = new RestaurantConfig();
    config.setOpenDaysList(java.util.List.of("FRIDAY", "SATURDAY", "SUNDAY"));

    assertTrue(config.isOpenOn(java.time.DayOfWeek.FRIDAY));
    assertTrue(config.isOpenOn(java.time.DayOfWeek.SATURDAY));
    assertTrue(config.isOpenOn(java.time.DayOfWeek.SUNDAY));
    assertFalse(config.isOpenOn(java.time.DayOfWeek.MONDAY));
    assertFalse(config.isOpenOn(java.time.DayOfWeek.TUESDAY));
  }

  @Test
  @DisplayName("Deve formatar o tempo estimado como faixa de tempo (tempo exato a +10min)")
  void testEstimatedTimeCalculation() {
    RestaurantConfig config = new RestaurantConfig();
    config.updateEstimate(45);

    assertEquals(Integer.valueOf(45), config.getEstimatedTimeMinutes());
    assertEquals("45 - 55 minutos", config.getEstimatedTimeText());

    // Teste com RestaurantConfigService
    com.restaurant.api.domain.repository.RestaurantConfigRepository mockRepo =
        org.mockito.Mockito.mock(com.restaurant.api.domain.repository.RestaurantConfigRepository.class);
    com.restaurant.api.domain.service.RestaurantConfigService service =
        new com.restaurant.api.domain.service.RestaurantConfigService(mockRepo);

    org.mockito.Mockito.when(mockRepo.findAll()).thenReturn(java.util.List.of(config));
    org.mockito.Mockito.when(mockRepo.save(org.mockito.ArgumentMatchers.any(RestaurantConfig.class)))
        .thenAnswer(i -> i.getArgument(0));

    com.restaurant.api.domain.dto.RestaurantConfigResponse response = service.updateEstimatedTime(50);
    assertEquals(Integer.valueOf(50), response.estimatedTimeMinutes());
    assertEquals("50 - 60 minutos", response.estimatedTimeText());
  }

  @Test
  @DisplayName("Deve calcular a data do expediente considerando virada de meia-noite")
  void testDailyOrderSequenceBusinessDateCalculation() {
    com.restaurant.api.domain.service.DailyOrderSequenceService service =
        new com.restaurant.api.domain.service.DailyOrderSequenceService(null);

    java.time.LocalDate baseDate = java.time.LocalDate.of(2026, 10, 17);
    java.time.LocalTime openTime = java.time.LocalTime.of(18, 0);
    java.time.LocalTime closeTime = java.time.LocalTime.of(2, 0); // Fecha às 02h da madrugada

    // 1. Pedido feito às 20h do dia 17 -> data é dia 17
    java.time.ZonedDateTime evening = java.time.ZonedDateTime.of(
        baseDate, java.time.LocalTime.of(20, 0), com.restaurant.api.domain.service.DailyOrderSequenceService.DEFAULT_ZONE
    );
    assertEquals(baseDate, service.calculateBusinessDate(evening, openTime, closeTime));

    // 2. Pedido feito à 01h30 da madrugada do dia 18 -> expediente pertence ao dia 17 (ontem)
    java.time.LocalDate nextDay = baseDate.plusDays(1);
    java.time.ZonedDateTime pastMidnight = java.time.ZonedDateTime.of(
        nextDay, java.time.LocalTime.of(1, 30), com.restaurant.api.domain.service.DailyOrderSequenceService.DEFAULT_ZONE
    );
    assertEquals(baseDate, service.calculateBusinessDate(pastMidnight, openTime, closeTime));

    // 3. Horário depois do fechamento (03h00 do dia 18) -> já é considerado dia 18
    java.time.ZonedDateTime afterClose = java.time.ZonedDateTime.of(
        nextDay, java.time.LocalTime.of(3, 0), com.restaurant.api.domain.service.DailyOrderSequenceService.DEFAULT_ZONE
    );
    assertEquals(nextDay, service.calculateBusinessDate(afterClose, openTime, closeTime));
  }

  @Test
  @DisplayName("Deve formatar e realizar parse de códigos diários de pedidos")
  void testOrderCodeFormattingAndParsing() {
    com.restaurant.api.domain.service.DailyOrderSequenceService service =
        new com.restaurant.api.domain.service.DailyOrderSequenceService(null);

    // Formatação com 4 dígitos e prefixo #
    assertEquals("#0001", service.formatOrderCode(1));
    assertEquals("#0015", service.formatOrderCode(15));
    assertEquals("#0120", service.formatOrderCode(120));
    assertEquals("#1050", service.formatOrderCode(1050));

    // Parsing seguro
    assertEquals(Integer.valueOf(1), service.parseSequenceNumber("#0001"));
    assertEquals(Integer.valueOf(15), service.parseSequenceNumber("#0015"));
    assertEquals(Integer.valueOf(42), service.parseSequenceNumber("42"));
    assertEquals(Integer.valueOf(99), service.parseSequenceNumber("  #0099  "));
    assertNull(service.parseSequenceNumber(null));
    assertNull(service.parseSequenceNumber(""));
    assertNull(service.parseSequenceNumber("ABC"));
  }

  @Test
  @DisplayName("Deve consolidar métricas financeiras, formas de pagamento e produtos no Relatório Diário")
  void testDailyReportConsolidation() {
    com.restaurant.api.domain.repository.OrderRepository orderRepo =
        org.mockito.Mockito.mock(com.restaurant.api.domain.repository.OrderRepository.class);
    com.restaurant.api.domain.repository.PaymentRepository paymentRepo =
        org.mockito.Mockito.mock(com.restaurant.api.domain.repository.PaymentRepository.class);
    com.restaurant.api.domain.repository.OrderItemRepository itemRepo =
        org.mockito.Mockito.mock(com.restaurant.api.domain.repository.OrderItemRepository.class);
    com.restaurant.api.domain.service.RestaurantConfigService configService =
        org.mockito.Mockito.mock(com.restaurant.api.domain.service.RestaurantConfigService.class);
    com.restaurant.api.domain.service.DailyOrderSequenceService sequenceService =
        org.mockito.Mockito.mock(com.restaurant.api.domain.service.DailyOrderSequenceService.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setDeliveryFee(new BigDecimal("5.00"));
    config.setOpenTime(java.time.LocalTime.of(18, 0));
    config.setCloseTime(java.time.LocalTime.of(23, 30));
    org.mockito.Mockito.when(configService.getConfigEntity()).thenReturn(config);

    java.time.LocalDate testDate = java.time.LocalDate.of(2026, 10, 17);
    org.mockito.Mockito.when(sequenceService.getBusinessDate(config)).thenReturn(testDate);

    // 1. Pedido Salão Fechado (ID 1)
    com.restaurant.api.domain.entity.Order order1 = new com.restaurant.api.domain.entity.Order();
    order1.setId(1L);
    order1.setStatus(com.restaurant.api.domain.enums.StatusOrder.CLOSED);
    order1.setOrderType(com.restaurant.api.domain.enums.OrderType.DINE_IN);
    order1.setOpenedAt(java.time.Instant.now());

    // 2. Pedido Delivery Entregue (ID 2)
    com.restaurant.api.domain.entity.Order order2 = new com.restaurant.api.domain.entity.Order();
    order2.setId(2L);
    order2.setStatus(com.restaurant.api.domain.enums.StatusOrder.DELIVERED);
    order2.setOrderType(com.restaurant.api.domain.enums.OrderType.DELIVERY);
    order2.setOpenedAt(java.time.Instant.now());

    // 3. Pedido Cancelado (ID 3)
    com.restaurant.api.domain.entity.Order order3 = new com.restaurant.api.domain.entity.Order();
    order3.setId(3L);
    order3.setStatus(com.restaurant.api.domain.enums.StatusOrder.CANCELLED);
    order3.setOpenedAt(java.time.Instant.now());

    org.mockito.Mockito.when(orderRepo.findByOpenedAtBetweenOrderByOpenedAtAsc(
        org.mockito.ArgumentMatchers.any(java.time.Instant.class),
        org.mockito.ArgumentMatchers.any(java.time.Instant.class)
    )).thenReturn(java.util.List.of(order1, order2, order3));

    // Pagamentos do Pedido 1: R$ 30 em Dinheiro + R$ 20 em PIX
    com.restaurant.api.domain.entity.Payment pay1 = new com.restaurant.api.domain.entity.Payment();
    pay1.setAmount(new BigDecimal("30.00"));
    pay1.setMethod(com.restaurant.api.domain.enums.PaymentMethod.CASH);
    pay1.setStatus(com.restaurant.api.domain.enums.StatusPayment.APPROVED);

    com.restaurant.api.domain.entity.Payment pay2 = new com.restaurant.api.domain.entity.Payment();
    pay2.setAmount(new BigDecimal("20.00"));
    pay2.setMethod(com.restaurant.api.domain.enums.PaymentMethod.PIX);
    pay2.setStatus(com.restaurant.api.domain.enums.StatusPayment.APPROVED);

    // Pagamento do Pedido 2: R$ 70 em Cartão de Crédito
    com.restaurant.api.domain.entity.Payment pay3 = new com.restaurant.api.domain.entity.Payment();
    pay3.setAmount(new BigDecimal("70.00"));
    pay3.setMethod(com.restaurant.api.domain.enums.PaymentMethod.CREDIT_CARD);
    pay3.setStatus(com.restaurant.api.domain.enums.StatusPayment.APPROVED);

    org.mockito.Mockito.when(paymentRepo.findByOrderIdInAndStatus(
        org.mockito.ArgumentMatchers.eq(java.util.List.of(1L, 2L)),
        org.mockito.ArgumentMatchers.eq(com.restaurant.api.domain.enums.StatusPayment.APPROVED)
    )).thenReturn(java.util.List.of(pay1, pay2, pay3));

    // Itens vendidos
    Product prodCalabresa = new Product();
    prodCalabresa.setId(10L);
    prodCalabresa.setName("Pizza Calabresa");
    prodCalabresa.setPrice(new BigDecimal("40.00"));

    com.restaurant.api.domain.entity.OrderItem item1 = new com.restaurant.api.domain.entity.OrderItem();
    item1.setOrder(order1);
    item1.setProduct(prodCalabresa);
    item1.setQuantity(2);
    item1.setUnitPrice(new BigDecimal("40.00"));

    org.mockito.Mockito.when(itemRepo.findValidItemsByOrderIds(
        org.mockito.ArgumentMatchers.eq(java.util.List.of(1L, 2L)),
        org.mockito.ArgumentMatchers.eq(com.restaurant.api.domain.enums.StatusOrderItem.CANCELLED)
    )).thenReturn(java.util.List.of(item1));

    com.restaurant.api.domain.service.ReportService reportService =
        new com.restaurant.api.domain.service.ReportService(orderRepo, paymentRepo, itemRepo, configService, sequenceService);

    com.restaurant.api.domain.dto.report.DailyReportResponse report = reportService.getDailyReport(testDate);

    assertNotNull(report);
    assertEquals(testDate, report.businessDate());
    assertEquals("17/10/2026", report.formattedDate());

    // Métricas operacionais
    assertEquals(3, report.operations().totalOrdersCreated());
    assertEquals(2, report.operations().completedOrders());
    assertEquals(1, report.operations().cancelledOrders());

    // Métricas financeiras (30 + 20 + 70 = 120 bruto)
    assertEquals(new BigDecimal("120.00"), report.financial().grossTotal());
    assertEquals(new BigDecimal("5.00"), report.financial().deliveryFeeTotal());
    assertEquals(new BigDecimal("115.00"), report.financial().netTotal());
    assertEquals(new BigDecimal("60.00"), report.financial().averageTicket()); // 120 / 2

    // Conferência de formas de pagamento
    java.util.Map<com.restaurant.api.domain.enums.PaymentMethod, BigDecimal> payMap = report.paymentMethods().stream()
        .collect(java.util.stream.Collectors.toMap(
            com.restaurant.api.domain.dto.report.PaymentMethodBreakdownDto::method,
            com.restaurant.api.domain.dto.report.PaymentMethodBreakdownDto::totalAmount
        ));
    assertEquals(new BigDecimal("30.00"), payMap.get(com.restaurant.api.domain.enums.PaymentMethod.CASH));
    assertEquals(new BigDecimal("20.00"), payMap.get(com.restaurant.api.domain.enums.PaymentMethod.PIX));
    assertEquals(new BigDecimal("70.00"), payMap.get(com.restaurant.api.domain.enums.PaymentMethod.CREDIT_CARD));
    assertEquals(new BigDecimal("0.00"), payMap.get(com.restaurant.api.domain.enums.PaymentMethod.DEBIT_CARD));

    // Top produtos
    assertFalse(report.topProducts().isEmpty());
    assertEquals("Pizza Calabresa", report.topProducts().get(0).productName());
    assertEquals(2, report.topProducts().get(0).quantitySold());
    assertEquals(new BigDecimal("80.00"), report.topProducts().get(0).totalAmount());
  }

  @Test
  @DisplayName("Deve calcular intervalo de expediente corretamente sem wrap-around quando fecha às 23:00")
  void testCalculateShiftIntervalLateClose() {
    com.restaurant.api.domain.service.ReportService reportService =
        new com.restaurant.api.domain.service.ReportService(null, null, null, null, null);

    RestaurantConfig config = new RestaurantConfig();
    config.setOpenTime(java.time.LocalTime.of(15, 0));
    config.setCloseTime(java.time.LocalTime.of(23, 0));

    java.time.LocalDate date = java.time.LocalDate.of(2026, 10, 17);
    java.time.Instant[] interval = reportService.calculateShiftInterval(date, config);

    assertNotNull(interval);
    assertEquals(2, interval.length);
    assertTrue(interval[0].isBefore(interval[1]), "shiftStart deve ser estritamente anterior a shiftEnd");

    // Intervalo do dia 17 deve cobrir 15:25 (quando o pedido foi feito)
    java.time.Instant orderInstant = java.time.ZonedDateTime.of(
        date, java.time.LocalTime.of(15, 25), java.time.ZoneId.of("America/Sao_Paulo")
    ).toInstant();

    assertTrue(orderInstant.isAfter(interval[0]) && orderInstant.isBefore(interval[1]),
        "O pedido feito às 15:25 deve estar contido dentro do turno");
  }

  @Test
  @DisplayName("Deve computar formas de pagamento a partir do pedido mesmo sem transações no gateway online")
  void testReportWithOrderPaymentMethodFallback() {
    com.restaurant.api.domain.repository.OrderRepository orderRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.OrderRepository.class);
    com.restaurant.api.domain.repository.PaymentRepository paymentRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.PaymentRepository.class);
    com.restaurant.api.domain.repository.OrderItemRepository itemRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.OrderItemRepository.class);
    com.restaurant.api.domain.service.RestaurantConfigService configService = org.mockito.Mockito.mock(com.restaurant.api.domain.service.RestaurantConfigService.class);
    com.restaurant.api.domain.service.DailyOrderSequenceService sequenceService = org.mockito.Mockito.mock(com.restaurant.api.domain.service.DailyOrderSequenceService.class);

    RestaurantConfig config = new RestaurantConfig();
    config.setOpenTime(java.time.LocalTime.of(15, 0));
    config.setCloseTime(java.time.LocalTime.of(23, 0));
    config.setDeliveryFee(new BigDecimal("2.00"));
    org.mockito.Mockito.when(configService.getConfigEntity()).thenReturn(config);

    java.time.LocalDate testDate = java.time.LocalDate.of(2026, 10, 17);

    // Pedido criado pelo cliente/balcão com Cartão de Crédito
    com.restaurant.api.domain.entity.Order order = new com.restaurant.api.domain.entity.Order();
    order.setId(100L);
    order.setStatus(com.restaurant.api.domain.enums.StatusOrder.DELIVERED);
    order.setOrderType(com.restaurant.api.domain.enums.OrderType.DELIVERY);
    order.setPaymentMethod(com.restaurant.api.domain.enums.PaymentMethod.CREDIT_CARD);
    order.setOpenedAt(java.time.Instant.now());

    org.mockito.Mockito.when(orderRepo.findByOpenedAtBetweenOrderByOpenedAtAsc(
        org.mockito.ArgumentMatchers.any(java.time.Instant.class),
        org.mockito.ArgumentMatchers.any(java.time.Instant.class)
    )).thenReturn(java.util.List.of(order));

    // Nenhuma transação no gateway
    org.mockito.Mockito.when(paymentRepo.findByOrderIdInAndStatus(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    )).thenReturn(java.util.Collections.emptyList());

    // Item: R$ 48.00
    Product pizza = new Product();
    pizza.setId(1L);
    pizza.setName("Pizza Especial");
    pizza.setPrice(new BigDecimal("48.00"));

    com.restaurant.api.domain.entity.OrderItem item = new com.restaurant.api.domain.entity.OrderItem();
    item.setOrder(order);
    item.setProduct(pizza);
    item.setQuantity(1);
    item.setUnitPrice(new BigDecimal("48.00"));

    org.mockito.Mockito.when(itemRepo.findValidItemsByOrderIds(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    )).thenReturn(java.util.List.of(item));

    com.restaurant.api.domain.service.ReportService reportService =
        new com.restaurant.api.domain.service.ReportService(orderRepo, paymentRepo, itemRepo, configService, sequenceService);

    com.restaurant.api.domain.dto.report.DailyReportResponse report = reportService.getDailyReport(testDate);

    // Total: R$ 48 + 2 (taxa) = 50.00
    assertEquals(new BigDecimal("50.00"), report.financial().grossTotal());
    assertEquals(new BigDecimal("2.00"), report.financial().deliveryFeeTotal());
    assertEquals(new BigDecimal("48.00"), report.financial().netTotal());

    // Forma de pagamento deve ser CREDIT_CARD = 50.00
    java.util.Map<com.restaurant.api.domain.enums.PaymentMethod, BigDecimal> payMap = report.paymentMethods().stream()
        .collect(java.util.stream.Collectors.toMap(
            com.restaurant.api.domain.dto.report.PaymentMethodBreakdownDto::method,
            com.restaurant.api.domain.dto.report.PaymentMethodBreakdownDto::totalAmount
        ));
    assertEquals(new BigDecimal("50.00"), payMap.get(com.restaurant.api.domain.enums.PaymentMethod.CREDIT_CARD));
  }

  @Test
  @DisplayName("Deve mapear imageUrl corretamente em Product, ProductRequest e ProductResponse")
  void testProductImageUrlMapping() {
    String sampleUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591";
    com.restaurant.api.domain.dto.ProductRequest request = new com.restaurant.api.domain.dto.ProductRequest(
        1L,
        "Pizza Margherita Especial",
        "Molho, mussarela e manjericão fresco",
        new BigDecimal("45.00"),
        true,
        true,
        30,
        java.util.Set.of(),
        java.util.List.of(),
        sampleUrl
    );

    Product product = request.toEntity(null, java.util.Set.of());
    assertEquals(sampleUrl, product.getImageUrl());

    com.restaurant.api.domain.dto.ProductResponse response = com.restaurant.api.domain.dto.ProductResponse.fromEntity(product);
    assertEquals(sampleUrl, response.imageUrl());
  }

  @Test
  @DisplayName("Deve permitir criar PizzaSize sem fatias (slices = null) para suportar bebidas e porcoes")
  void testPizzaSizeWithOptionalSlicesAndDefaultFlavors() {
    com.restaurant.api.domain.dto.PizzaSizeRequest request = new com.restaurant.api.domain.dto.PizzaSizeRequest(
        "Lata 350ml",
        "350ML",
        null,
        null,
        new BigDecimal("6.00"),
        true
    );

    assertEquals("Lata 350ml", request.name());
    assertEquals("350ML", request.shortName());
    assertNull(request.slices());
    assertNull(request.maxFlavors());

    // Usando construtor de compatibilidade
    com.restaurant.api.domain.dto.PizzaSizeRequest compatRequest = new com.restaurant.api.domain.dto.PizzaSizeRequest(
        "Garrafa 2L",
        "2L",
        null,
        null,
        true
    );
    assertNull(compatRequest.slices());
    assertEquals(1, compatRequest.maxFlavors());
    assertEquals(BigDecimal.ZERO, compatRequest.price());

    // Entidade
    com.restaurant.api.domain.entity.PizzaSize size = new com.restaurant.api.domain.entity.PizzaSize();
    size.setName(request.name());
    size.setShortName(request.shortName());
    size.setSlices(request.slices());
    size.setMaxFlavors(request.maxFlavors() != null ? request.maxFlavors() : 1);
    size.setPrice(request.price());
    size.setActive(request.active());

    assertNull(size.getSlices());
    assertEquals(1, size.getMaxFlavors());
    assertEquals(new BigDecimal("6.00"), size.getPrice());

    // Response DTO
    com.restaurant.api.domain.dto.PizzaSizeResponse response = com.restaurant.api.domain.dto.PizzaSizeResponse.fromEntity(size);
    assertEquals("Lata 350ml", response.name());
    assertNull(response.slices());
    assertEquals(1, response.maxFlavors());
    assertEquals(new BigDecimal("6.00"), response.price());
  }

  @Test
  @DisplayName("Deve validar criacao de OrderRequest com multiplos pagamentos (split payments)")
  void testOrderRequestWithMultiplePayments() {
    com.restaurant.api.domain.dto.OrderPaymentRequest p1 = new com.restaurant.api.domain.dto.OrderPaymentRequest(
        com.restaurant.api.domain.enums.PaymentMethod.PIX,
        new BigDecimal("30.00"),
        "Joao"
    );
    com.restaurant.api.domain.dto.OrderPaymentRequest p2 = new com.restaurant.api.domain.dto.OrderPaymentRequest(
        com.restaurant.api.domain.enums.PaymentMethod.CASH,
        new BigDecimal("20.00"),
        "Maria"
    );

    com.restaurant.api.domain.dto.OrderRequest request = new com.restaurant.api.domain.dto.OrderRequest(
        1L,
        com.restaurant.api.domain.enums.OrderType.DINE_IN,
        null,
        null,
        false,
        com.restaurant.api.domain.enums.PaymentMethod.PIX,
        null,
        "Observacao mesa 1",
        java.util.List.of(),
        "#01",
        java.util.List.of(p1, p2)
    );

    assertNotNull(request.payments());
    assertEquals(2, request.payments().size());
    assertEquals(new BigDecimal("30.00"), request.payments().get(0).amount());
    assertEquals(com.restaurant.api.domain.enums.PaymentMethod.PIX, request.payments().get(0).method());
    assertEquals(new BigDecimal("20.00"), request.payments().get(1).amount());
    assertEquals(com.restaurant.api.domain.enums.PaymentMethod.CASH, request.payments().get(1).method());
  }

  @Test
  @DisplayName("Deve validar DTO OrderPaymentMethodUpdateRequest e validacao de troco")
  void testOrderPaymentMethodUpdateRequest() {
    com.restaurant.api.domain.dto.OrderPaymentMethodUpdateRequest req =
        new com.restaurant.api.domain.dto.OrderPaymentMethodUpdateRequest(
            com.restaurant.api.domain.enums.PaymentMethod.CASH,
            new BigDecimal("50.00")
        );

    assertEquals(com.restaurant.api.domain.enums.PaymentMethod.CASH, req.paymentMethod());
    assertEquals(new BigDecimal("50.00"), req.cashChangeFor());
  }

  @Test
  @DisplayName("Deve validar DTO OrderGeneralUpdateRequest para notas, mesa e endereco")
  void testOrderGeneralUpdateRequest() {
    com.restaurant.api.domain.dto.DeliveryAddressDto addr = new com.restaurant.api.domain.dto.DeliveryAddressDto(
        "Rua Principal", "100", "Centro", "Apto 1", "Perto da praca"
    );
    com.restaurant.api.domain.dto.OrderGeneralUpdateRequest req =
        new com.restaurant.api.domain.dto.OrderGeneralUpdateRequest(
            "Sem cebola por favor",
            5L,
            10L,
            addr
        );

    assertEquals("Sem cebola por favor", req.note());
    assertEquals(5L, req.tableId());
    assertEquals(10L, req.addressId());
    assertNotNull(req.deliveryAddress());
    assertEquals("Rua Principal", req.deliveryAddress().street());
  }

  @Test
  @DisplayName("Deve suportar clientOrderId em OrderRequest e Order entity para idempotencia")
  void testClientOrderIdSupport() {
    com.restaurant.api.domain.dto.OrderRequest request = new com.restaurant.api.domain.dto.OrderRequest(
        null, com.restaurant.api.domain.enums.OrderType.TAKEOUT, null, null, false,
        com.restaurant.api.domain.enums.PaymentMethod.PIX, null, "Observacao",
        java.util.List.of(), "#0001", null, "CLIENT-UUID-123"
    );

    assertEquals("CLIENT-UUID-123", request.clientOrderId());

    com.restaurant.api.domain.dto.OrderRequest updated = request.withClientOrderId("CLIENT-UUID-456");
    assertEquals("CLIENT-UUID-456", updated.clientOrderId());
    assertEquals("Observacao", updated.note());
    assertEquals("#0001", updated.orderCode());

    com.restaurant.api.domain.entity.Order order = new com.restaurant.api.domain.entity.Order();
    order.setClientOrderId("CLIENT-UUID-789");
    assertEquals("CLIENT-UUID-789", order.getClientOrderId());
  }

  @Test
  @DisplayName("Deve suportar customerName e customerPhone no OrderRequest, Order e OrderResponse")
  void testCustomerFieldsInOrderAndResponse() {
    com.restaurant.api.domain.dto.OrderRequest request = new com.restaurant.api.domain.dto.OrderRequest(
        null, com.restaurant.api.domain.enums.OrderType.DELIVERY, null, null, false,
        com.restaurant.api.domain.enums.PaymentMethod.PIX, null, "Observacao",
        java.util.List.of(), "#0002", null, "CLIENT-UUID-999",
        "Tatiane Aparecida Alves", "38992695661"
    );

    assertEquals("Tatiane Aparecida Alves", request.customerName());
    assertEquals("38992695661", request.customerPhone());

    com.restaurant.api.domain.entity.Order order = new com.restaurant.api.domain.entity.Order();
    order.setCustomerName("Tatiane Aparecida Alves");
    order.setCustomerPhone("38992695661");

    com.restaurant.api.domain.dto.OrderResponse response = com.restaurant.api.domain.dto.OrderResponse.fromEntity(order);
    assertEquals("Tatiane Aparecida Alves", response.customerName());
    assertEquals("38992695661", response.customerPhone());

    // Fallback: quando customerName na Order for nulo, deve pegar do User
    com.restaurant.api.domain.entity.Order orderLegacy = new com.restaurant.api.domain.entity.Order();
    com.restaurant.api.domain.entity.User user = new com.restaurant.api.domain.entity.User();
    user.setName("Carlos Legacy");
    user.setNumber("38999991111");
    orderLegacy.setUser(user);

    com.restaurant.api.domain.dto.OrderResponse legacyResponse = com.restaurant.api.domain.dto.OrderResponse.fromEntity(orderLegacy);
    assertEquals("Carlos Legacy", legacyResponse.customerName());
    assertEquals("38999991111", legacyResponse.customerPhone());
  }

  @Test
  @DisplayName("Deve atualizar o nome do visitante em AuthService.loginGuest quando novo nome for informado")
  void testAuthServiceLoginGuestUpdatesName() {
    com.restaurant.api.domain.repository.UserRepository userRepo = org.mockito.Mockito.mock(com.restaurant.api.domain.repository.UserRepository.class);
    com.restaurant.api.domain.infra.security.TokenService tokenService = org.mockito.Mockito.mock(com.restaurant.api.domain.infra.security.TokenService.class);
    org.springframework.security.authentication.AuthenticationManager authManager = org.mockito.Mockito.mock(org.springframework.security.authentication.AuthenticationManager.class);

    com.restaurant.api.domain.service.AuthService authService = new com.restaurant.api.domain.service.AuthService(
        userRepo, authManager, tokenService
    );

    com.restaurant.api.domain.entity.User existingUser = new com.restaurant.api.domain.entity.User();
    existingUser.setId(10L);
    existingUser.setLogin("GUEST-38992695661");
    existingUser.setName("Cliente");
    existingUser.setNumber("38992695661");
    existingUser.setRole(com.restaurant.api.domain.enums.Role.CUSTOMER);

    org.mockito.Mockito.when(userRepo.findByLogin("GUEST-38992695661")).thenReturn(existingUser);
    org.mockito.Mockito.when(tokenService.generateToken(org.mockito.ArgumentMatchers.any())).thenReturn("dummy-jwt-token");

    com.restaurant.api.domain.dto.GuestRequest guestReq = new com.restaurant.api.domain.dto.GuestRequest("Tatiane Alves", "38992695661");
    com.restaurant.api.domain.dto.UserResponse res = authService.loginGuest(guestReq);

    assertEquals("Tatiane Alves", existingUser.getName());
    assertEquals("Tatiane Alves", res.name());
    org.mockito.Mockito.verify(userRepo).save(existingUser);
  }
}
