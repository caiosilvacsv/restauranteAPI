package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.RestaurantConfigRequest;
import com.restaurant.api.domain.dto.RestaurantConfigResponse;
import com.restaurant.api.domain.service.RestaurantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@Tag(name = "Configuração do Restaurante", description = "Endpoints para consulta e atualização de horários, taxas de entrega e status da loja")
public class RestaurantConfigController {

  private final RestaurantConfigService service;

  public RestaurantConfigController(RestaurantConfigService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Obter configurações da loja e horários")
  public ResponseEntity<RestaurantConfigResponse> getConfig() {
    return ResponseEntity.ok(service.getConfig());
  }

  @PutMapping
  @Operation(summary = "Atualizar configurações do restaurante (Admin)")
  public ResponseEntity<RestaurantConfigResponse> updateConfig(@RequestBody RestaurantConfigRequest request) {
    return ResponseEntity.ok(service.updateConfig(request));
  }

  @PatchMapping("/estimate")
  @Operation(summary = "Atualizar tempo estimado padrão da loja (Admin e Garçom)")
  public ResponseEntity<RestaurantConfigResponse> updateEstimate(
      @RequestBody @jakarta.validation.Valid com.restaurant.api.domain.dto.OrderEstimateRequest request
  ) {
    return ResponseEntity.ok(service.updateEstimatedTime(request.estimatedMinutes()));
  }
}
