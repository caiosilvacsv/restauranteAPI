package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.KitchenItemResponse;
import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.StatusOrderItem;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KitchenService {
  private final OrderItemRepository orderItemRepository;

  public KitchenService(OrderItemRepository orderItemRepository) {
    this.orderItemRepository = orderItemRepository;
  }

  public List<KitchenItemResponse> listAllKitchenItemsPending() {
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.PENDING)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  public List<KitchenItemResponse> listAllKitchenItemsPreparing() {
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.PREPARING)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  public List<KitchenItemResponse> listAllKitchenItemsQueued() {
    return orderItemRepository.findByStatusOrderByIdAsc(StatusOrderItem.QUEUED)
        .stream()
        .map(KitchenItemResponse::fromEntity)
        .toList();
  }

  // Compatibilidade com código legado
  public List<KitchenItemResponse> ListAllKitchenItemsPending() {
    return listAllKitchenItemsPending();
  }

  public List<KitchenItemResponse> ListAllKitchenItemsPreparing() {
    return listAllKitchenItemsPreparing();
  }

  public List<KitchenItemResponse> ListAllKitchenItemsQueued() {
    return listAllKitchenItemsQueued();
  }

  @Transactional
  public KitchenItemResponse addToQueued(Long itemId) {
    OrderItem item = findItemById(itemId);

    if (item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item cancelado", "Não é possível enfileirar um item cancelado.");
    }

    // Idempotência: se já está enfileirado ou em etapa posterior, retorna sem erro
    if (item.getStatus().ordinal() >= StatusOrderItem.QUEUED.ordinal()) {
      return KitchenItemResponse.fromEntity(item);
    }

    item.setStatus(StatusOrderItem.QUEUED);
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  @Transactional
  public KitchenItemResponse initPreparing(Long itemId) {
    OrderItem item = findItemById(itemId);

    if (item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item cancelado", "Não é possível preparar um item cancelado.");
    }

    // Idempotência: se já está em preparo ou etapa posterior, retorna sem erro
    if (item.getStatus().ordinal() >= StatusOrderItem.PREPARING.ordinal()) {
      return KitchenItemResponse.fromEntity(item);
    }

    item.setStatus(StatusOrderItem.PREPARING);
    if (item.getPreparationStartedAt() == null) {
      item.initPrepared();
    }
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  @Transactional
  public KitchenItemResponse markReady(Long itemId) {
    OrderItem item = findItemById(itemId);

    if (item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item cancelado", "Não é possível finalizar um item cancelado.");
    }

    // Idempotência: se já está pronto ou etapa posterior, retorna sem erro
    if (item.getStatus().ordinal() >= StatusOrderItem.READY.ordinal()) {
      return KitchenItemResponse.fromEntity(item);
    }

    item.setStatus(StatusOrderItem.READY);
    if (item.getReadyAt() == null) {
      item.isReady();
    }
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  @Transactional
  public KitchenItemResponse initDespatched(Long itemId) {
    OrderItem item = findItemById(itemId);

    if (item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item cancelado", "Não é possível despachar um item cancelado.");
    }

    // Idempotência: se já foi despachado ou entregue, retorna sem erro
    if (item.getStatus().ordinal() >= StatusOrderItem.DESPATCHED.ordinal()) {
      return KitchenItemResponse.fromEntity(item);
    }

    item.setStatus(StatusOrderItem.DESPATCHED);
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  @Transactional
  public KitchenItemResponse deliveredItem(Long itemId) {
    OrderItem item = findItemById(itemId);

    if (item.getStatus() == StatusOrderItem.CANCELLED) {
      throw new BusinessRulesException("Item cancelado", "Não é possível entregar um item cancelado.");
    }

    // Idempotência: se já foi entregue ou fechado, retorna sem erro
    if (item.getStatus().ordinal() >= StatusOrderItem.DELIVERED.ordinal()) {
      return KitchenItemResponse.fromEntity(item);
    }

    item.setStatus(StatusOrderItem.DELIVERED);
    if (item.getDeliveredAt() == null) {
      item.delivered();
    }
    return KitchenItemResponse.fromEntity(orderItemRepository.save(item));
  }

  private OrderItem findItemById(Long itemId) {
    return orderItemRepository.findById(itemId)
        .orElseThrow(() -> new NotFoundException(
            "Order item not found.", "Order item not found with ID: " + itemId));
  }
}
