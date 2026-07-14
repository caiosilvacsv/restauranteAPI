package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.OrderItemRequest;
import com.restaurant.api.domain.dto.OrderItemResponse;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {this.service = service;}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse register(@RequestBody OrderRequest order) {
    return service.createOrder(order);
  }

  @GetMapping
  public Page<OrderResponse> getOrders(Pageable pageable) {
    return service.findAll(pageable);
  }

  @GetMapping("/{id}")
  public OrderResponse findById(@RequestParam Long id) {
    return service.findById(id);
  }

  @PostMapping("{orderId}/items")
  @ResponseStatus(HttpStatus.CREATED)
  public OrderItemResponse addItem(
      @PathVariable Long orderId,
      @RequestBody OrderItemRequest request
  ) {
    return service.addItem(orderId, request);
  }

  @GetMapping("/{orderId}/items")
  public List<OrderItemResponse> itemsList(@PathVariable Long orderId) {
    return service.itemsList(orderId);
  }
}
