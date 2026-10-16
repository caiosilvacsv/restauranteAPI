package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.RestaurantConfigRequest;
import com.restaurant.api.domain.dto.RestaurantConfigResponse;
import com.restaurant.api.domain.service.RestaurantConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
public class RestaurantConfigController {

  private final RestaurantConfigService service;

  public RestaurantConfigController(RestaurantConfigService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<RestaurantConfigResponse> getConfig() {
    return ResponseEntity.ok(service.getConfig());
  }

  @PutMapping
  public ResponseEntity<RestaurantConfigResponse> updateConfig(@RequestBody RestaurantConfigRequest request) {
    return ResponseEntity.ok(service.updateConfig(request));
  }

  @PatchMapping("/estimate")
  public ResponseEntity<RestaurantConfigResponse> updateEstimate(
      @RequestBody @jakarta.validation.Valid com.restaurant.api.domain.dto.OrderEstimateRequest request
  ) {
    return ResponseEntity.ok(service.updateEstimatedTime(request.estimatedMinutes()));
  }
}
