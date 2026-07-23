package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.InvoiceRequest;
import com.restaurant.api.domain.dto.InvoiceResponse;
import com.restaurant.api.domain.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/{orderId}/invoice")
public class InvoiceController {
  private final InvoiceService service;

  public InvoiceController(InvoiceService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InvoiceResponse invoice(
      @PathVariable Long orderId,
      @RequestBody InvoiceRequest request) {
    return service.createInvoice(orderId, request);
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public InvoiceResponse invoice(@PathVariable Long orderId) {
    return service.findByOrder(orderId);
  }

}
