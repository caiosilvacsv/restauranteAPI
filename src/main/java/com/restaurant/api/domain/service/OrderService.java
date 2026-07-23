package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.OrderItemRequest;
import com.restaurant.api.domain.dto.OrderItemResponse;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.entity.*;
import com.restaurant.api.domain.enums.ModificationType;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

  private final OrderRepository orderRepository;

  private final RestaurantTableRepository tableRepository;

  private final ProductRepository productRepository;

  private final OrderItemRepository orderItemRepository;

  private final IngredientRepository ingredientRepository;

  public OrderService(
      OrderRepository orderRepository,
      RestaurantTableRepository tableRepository,
      ProductRepository productRepository,
      OrderItemRepository orderItemRepository,
      IngredientRepository ingredientRepository
  ) {
    this.orderRepository = orderRepository;
    this.tableRepository = tableRepository;
    this.productRepository = productRepository;
    this.orderItemRepository = orderItemRepository;
    this.ingredientRepository = ingredientRepository;
  }

  public OrderResponse createOrder(OrderRequest request) {
    RestaurantTables table = tableRepository.findById(request.id())
        .orElseThrow(()-> new NotFoundException(
            "Table not Found","Could not find table with ID: " + request.id()));

    if(!table.getStatus().isAvailable())
      throw new BusinessRulesException("Table is not available");

    Order order = new Order(table, request.note());

    table.occupyTable();

    Order saveOrder = orderRepository.save(order);
    tableRepository.save(table);
    return OrderResponse.fromEntity(saveOrder);
  }

  public Page<OrderResponse> findAll(Pageable pageable) {
    return  orderRepository.findAll(pageable).map(OrderResponse::fromEntity);
  }

  public OrderResponse findById(Long id) {
    return OrderResponse.fromEntity(findOrderById(id));
  }

  public OrderItemResponse addItem(Long id, OrderItemRequest request) {
    if (request.quantity() == null || request.quantity() <= 0)
      throw new BusinessRulesException("Invalid quantity");

    if (request.productId() == null)
      throw new BusinessRulesException("Invalid product id");

    Order order = findOrderById(id);

    if (order.getStatus().isOpen())
      throw new BusinessRulesException("Order is not opened");


    Product product = productRepository.findById(request.productId())
        .orElseThrow(()-> new NotFoundException(
            "Product not found",
            "Product not found with ID: " + request.productId()
        ));

    if (!product.getActive())
      throw new BusinessRulesException("Product is not available in menu!");

    OrderItem items = new OrderItem();

    items.setOrderID(order);
    items.setProductID(product);
    items.setQuantity(request.quantity());
    items.setNotes(request.note());

    BigDecimal finalUnitPrice = product.getPrice();

    //Process ingredients
    if (request.addIngredients() != null && !request.addIngredients().isEmpty()){
      List<Ingredient> ingredientsToAdd = ingredientRepository
          .findAllById(request.addIngredients());

      for (Ingredient ingredient : ingredientsToAdd){
        OrderItemModification mod = new OrderItemModification();
        mod.setOrderItem(items);
        mod.setIngredient(ingredient);
        mod.setModificationType(ModificationType.ADD);
        mod.setExtraPrice(ingredient.getPrice());
        items.getModifications().add(mod);
        finalUnitPrice = finalUnitPrice.add(ingredient.getPrice());
      }
    }

    if (request.removeIngredients() != null && !request.removeIngredients().isEmpty()){
      List<Ingredient> ingredientsToRemove = ingredientRepository
          .findAllById(request.removeIngredients());
      for (Ingredient ingredient : ingredientsToRemove){
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

  public List<OrderItemResponse> itemsList(Long orderId){
    Order order = findOrderById(orderId);
    return orderItemRepository.findByOrderId(order.getId()).stream().map(OrderItemResponse::fromEntity).toList();
  }

  private Order findOrderById(Long orderId){
    return orderRepository.findById(orderId)
        .orElseThrow(()-> new NotFoundException("Order not found",
            "Order not found with ID: " + orderId));
  }
}
