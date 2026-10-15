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
}
