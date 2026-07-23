package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.KitchenItemResponse;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.OrderItemRepository;
import org.hibernate.query.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KitchenService {
  private final OrderItemRepository orderItemRepository;

  public KitchenService(OrderItemRepository orderItemRepository) {
    this.orderItemRepository = orderItemRepository;
  }

  public List<KitchenItemResponse> ListAllKitchenItemsPending() {
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.PENDING)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  public List<KitchenItemResponse> ListAllKitchenItemsPreparing() {
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.PREPARING)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  public List<KitchenItemResponse> ListAllKitchenItemsQueued(){
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.QUEUED)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  public KitchenItemResponse addToQueued(Long orderId) {
    OrderItem item = findItemById(orderId);

    if (!item.getStatus().canTransitionTo(StatusOrderItem.QUEUED))
      throw new BusinessRulesException("Incorrect order status", "The order must be pending for in the queued.");

    item.next();
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  public KitchenItemResponse initPreparing(Long orderId) {
    OrderItem item = findItemById(orderId);

    if (!item.getStatus().canTransitionTo(StatusOrderItem.PREPARING))
      throw new BusinessRulesException("Incorrect order status", "The order must be queued for in the preparing.");

    item.next();
    item.initPrepared();
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  public KitchenItemResponse markReady(Long orderId) {
    OrderItem item = findItemById(orderId);

    if (!item.getStatus().canTransitionTo(StatusOrderItem.READY))
      throw new BusinessRulesException("Incorrect order status", "The order must be preparing for in the ready.");

    item.next();
    item.isReady();
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  public KitchenItemResponse initDespatched(Long orderId) {
    OrderItem item = findItemById(orderId);

    if (!item.getStatus().canTransitionTo(StatusOrderItem.DESPATCHED))
      throw new BusinessRulesException("Incorrect order status", "The order must be ready for in the despatched.");

    item.next();
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  public KitchenItemResponse deliveredItem(Long orderId) {
    OrderItem item = findItemById(orderId);

    if (!item.getStatus().canTransitionTo(StatusOrderItem.DELIVERED))
      throw new BusinessRulesException("Incorrect order status", "The order must be despatched for in the delivered item.");

    item.next();
    item.delivered();
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  private OrderItem findItemById(Long orderId) {
    return orderItemRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException(
            "Order item not found.", "Order item not found with ID: " + orderId));
  }

}
