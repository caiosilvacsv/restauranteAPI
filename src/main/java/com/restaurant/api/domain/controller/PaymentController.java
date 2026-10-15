package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.OrderPaymentRequest;
import com.restaurant.api.domain.dto.OrderPaymentStatusResponse;
import com.restaurant.api.domain.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/{orderId}/payments")
@Tag(name = "Pagamentos de Pedidos", description = "Endpoints para registro de pagamentos únicos ou múltiplos/divididos de comandas e pedidos")
public class PaymentController {

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Registrar pagamento (parcial ou total)", description = "Permite registrar um pagamento por pessoa/método. Abate o saldo devedor e encerra o pedido quando quitado.")
  public ResponseEntity<OrderPaymentStatusResponse> addPayment(
      @PathVariable Long orderId,
      @Valid @RequestBody OrderPaymentRequest request
  ) {
    return ResponseEntity.ok(service.registerPayment(orderId, request));
  }

  @GetMapping
  @Operation(summary = "Consultar extrato de pagamentos e saldo restante")
  public ResponseEntity<OrderPaymentStatusResponse> getPaymentStatus(@PathVariable Long orderId) {
    return ResponseEntity.ok(service.getOrderPaymentStatus(orderId));
  }
}
