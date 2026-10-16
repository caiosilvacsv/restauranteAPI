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
    assertEquals(BigDecimal.valueOf(40.00), item.calculateUnitPrice());
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
}
