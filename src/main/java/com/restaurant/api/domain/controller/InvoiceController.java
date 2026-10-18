package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.InvoiceRequest;
import com.restaurant.api.domain.dto.InvoiceResponse;
import com.restaurant.api.domain.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/{orderId}/invoice")
@Tag(name = "Faturas e Contabilidade", description = "Endpoints para emissão de fechamento contábil e faturas de pedidos")
public class InvoiceController {
  private final InvoiceService service;

  public InvoiceController(InvoiceService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Gerar fatura / conferência contábil de fechamento do pedido")
  public InvoiceResponse invoice(
      @PathVariable Long orderId,
      @RequestBody InvoiceRequest request) {
    return service.createInvoice(orderId, request);
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Consultar fatura existente do pedido")
  public InvoiceResponse invoice(@PathVariable Long orderId) {
    return service.findByOrder(orderId);
  }
}
