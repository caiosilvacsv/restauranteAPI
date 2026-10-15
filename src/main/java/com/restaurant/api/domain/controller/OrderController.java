package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.OrderEstimateRequest;
import com.restaurant.api.domain.dto.OrderItemRequest;
import com.restaurant.api.domain.dto.OrderItemResponse;
import com.restaurant.api.domain.dto.OrderRequest;
import com.restaurant.api.domain.dto.OrderResponse;
import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Pedidos", description = "Endpoints para criação, consulta, avanço de etapas, cancelamento e estimativas de pedidos")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Criar novo pedido (Delivery, Balcão ou Mesa)")
  public ResponseEntity<OrderResponse> register(@RequestBody OrderRequest order) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(order));
  }

  @GetMapping
  @Operation(summary = "Listar todos os pedidos (Admin e Garçom/Hub)", 
             description = "Permite filtrar alterações recentes (?since=ISO_TIMESTAMP), por dia (?date=YYYY-MM-DD), status ou mesa.")
  public ResponseEntity<Page<OrderResponse>> getOrders(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) StatusOrder status,
      @RequestParam(required = false) Long tableId,
      Pageable pageable
  ) {
    return ResponseEntity.ok(service.findAll(since, date, status, tableId, pageable));
  }

  @GetMapping("/me")
  @Operation(summary = "Meus Pedidos (Cliente logado)", description = "Retorna o histórico e status dos pedidos do cliente autenticado")
  public ResponseEntity<Page<OrderResponse>> getMyOrders(Pageable pageable) {
    return ResponseEntity.ok(service.findMyOrders(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar pedido por ID", description = "Clientes comuns só conseguem visualizar seus próprios pedidos. Admin e Garçom têm acesso a qualquer pedido.")
  public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Atualizar status do pedido (Admin e Garçom/Hub)", description = "Permite avançar ou pular o ciclo de vida do pedido (PREPARING, READY, DESPATCHED, DELIVERED, CLOSED, etc.)")
  public ResponseEntity<OrderResponse> updateStatus(
      @PathVariable Long id,
      @RequestParam StatusOrder status
  ) {
    return ResponseEntity.ok(service.updateStatus(id, status));
  }

  @PatchMapping("/{id}/estimate")
  @Operation(summary = "Definir tempo estimado de entrega (Hub local / Garçom)", description = "Atualiza os minutos estimados e calcula o horário previsto de entrega")
  public ResponseEntity<OrderResponse> updateEstimate(
      @PathVariable Long id,
      @Valid @RequestBody OrderEstimateRequest request
  ) {
    return ResponseEntity.ok(service.updateEstimate(id, request.estimatedMinutes()));
  }

  @PatchMapping("/{id}/cancel")
  @Operation(summary = "Cancelar pedido", description = "Cliente pode cancelar apenas enquanto o pedido estiver OPENED. Admin e Garçom podem cancelar em qualquer etapa com motivo.")
  public ResponseEntity<OrderResponse> cancelOrder(
      @PathVariable Long id,
      @RequestParam(required = false) String reason
  ) {
    return ResponseEntity.ok(service.cancelOrder(id, reason));
  }

  @PostMapping("/{orderId}/items")
  @Operation(summary = "Adicionar item à comanda de mesa ou pedido aberto")
  public ResponseEntity<OrderItemResponse> addItem(
      @PathVariable Long orderId,
      @RequestBody OrderItemRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(orderId, request));
  }

  @GetMapping("/{orderId}/items")
  @Operation(summary = "Listar itens de um pedido")
  public ResponseEntity<List<OrderItemResponse>> itemsList(@PathVariable Long orderId) {
    return ResponseEntity.ok(service.itemsList(orderId));
  }
}
