package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.TableRequest;
import com.restaurant.api.domain.dto.TableResponse;
import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.entity.RestaurantTables;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.enums.StatusTable;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.OrderRepository;
import com.restaurant.api.domain.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RestaurantTableService {

  private final RestaurantTableRepository tableRepository;
  private final OrderRepository orderRepository;

  private static final Set<StatusOrder> FINISHED_STATUSES = Set.of(StatusOrder.CLOSED, StatusOrder.CANCELLED);

  public RestaurantTableService(RestaurantTableRepository tableRepository, OrderRepository orderRepository) {
    this.tableRepository = tableRepository;
    this.orderRepository = orderRepository;
  }

  public List<TableResponse> findAll() {
    return tableRepository.findAllByOrderByNumberAsc()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public List<TableResponse> findAvailable() {
    return tableRepository.findByStatusOrderByNumberAsc(StatusTable.AVAILABLE)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public TableResponse findById(Long id) {
    RestaurantTables table = tableRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Mesa não encontrada", "ID da mesa: " + id));
    return toResponse(table);
  }

  @Transactional
  public TableResponse create(TableRequest request) {
    if (tableRepository.existsByNumber(request.number())) {
      throw new BusinessRulesException("Mesa já cadastrada", "Já existe uma mesa com o número " + request.number());
    }

    RestaurantTables table = new RestaurantTables();
    table.setNumber(request.number());
    table.setDescription(request.description());
    table.setCapacity(request.capacity());
    table.setStatus(request.status() != null ? request.status() : StatusTable.AVAILABLE);

    return toResponse(tableRepository.save(table));
  }

  @Transactional
  public TableResponse update(Long id, TableRequest request) {
    RestaurantTables table = tableRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Mesa não encontrada", "ID da mesa: " + id));

    tableRepository.findByNumber(request.number())
        .filter(t -> !t.getId().equals(id))
        .ifPresent(t -> {
          throw new BusinessRulesException("Número em uso", "Já existe outra mesa com o número " + request.number());
        });

    table.setNumber(request.number());
    table.setDescription(request.description());
    table.setCapacity(request.capacity());
    if (request.status() != null) {
      table.setStatus(request.status());
    }

    return toResponse(tableRepository.save(table));
  }

  @Transactional
  public TableResponse updateStatus(Long id, StatusTable status) {
    RestaurantTables table = tableRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Mesa não encontrada", "ID da mesa: " + id));

    table.setStatus(status);
    return toResponse(tableRepository.save(table));
  }

  private TableResponse toResponse(RestaurantTables table) {
    Long currentOrderId = null;
    String currentOrderCode = null;
    java.time.Instant openedAt = null;

    if (table.getStatus() == StatusTable.OCCUPIED) {
      Optional<Order> activeOrder = orderRepository.findFirstByTableIdAndStatusNotIn(table.getId(), FINISHED_STATUSES);
      if (activeOrder.isPresent()) {
        Order o = activeOrder.get();
        currentOrderId = o.getId();
        currentOrderCode = o.getOrderCode();
        openedAt = o.getOpenedAt();
      }
    }

    return TableResponse.fromEntity(table, currentOrderId, currentOrderCode, openedAt);
  }
}
