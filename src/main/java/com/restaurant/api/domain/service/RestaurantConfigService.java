package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.RestaurantConfigRequest;
import com.restaurant.api.domain.dto.RestaurantConfigResponse;
import com.restaurant.api.domain.entity.RestaurantConfig;
import com.restaurant.api.domain.repository.RestaurantConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

@Service
public class RestaurantConfigService {

  private final RestaurantConfigRepository repository;

  public RestaurantConfigService(RestaurantConfigRepository repository) {
    this.repository = repository;
  }

  public RestaurantConfig getConfigEntity() {
    return repository.findAll().stream().findFirst().orElseGet(() -> {
      RestaurantConfig defaultConfig = new RestaurantConfig();
      defaultConfig.setRestaurantName("La Bella Pizzaria");
      defaultConfig.setWhatsappPhone("5538991570684");
      defaultConfig.setPixKey("38991570684");
      defaultConfig.setTypePixKey("TELEFONE");
      defaultConfig.setDeliveryFee(BigDecimal.valueOf(2.00));
      defaultConfig.setAddress("Taiobeiras - MG");
      defaultConfig.setOpenTime(LocalTime.of(18, 0));
      defaultConfig.setCloseTime(LocalTime.of(23, 30));
      defaultConfig.setManuallyClosed(false);
      defaultConfig.setClosedMessage("Estamos fechados no momento. Não estamos recebendo pedidos!");
      return repository.save(defaultConfig);
    });
  }

  public RestaurantConfigResponse getConfig() {
    return RestaurantConfigResponse.fromEntity(getConfigEntity());
  }

  @Transactional
  public RestaurantConfigResponse updateConfig(RestaurantConfigRequest request) {
    RestaurantConfig config = getConfigEntity();

    if (request.restaurantName() != null && !request.restaurantName().isBlank()) {
      config.setRestaurantName(request.restaurantName());
    }
    if (request.whatsappPhone() != null && !request.whatsappPhone().isBlank()) {
      config.setWhatsappPhone(request.whatsappPhone());
    }
    if (request.pixKey() != null) {
      config.setPixKey(request.pixKey());
    }
    if (request.typePixKey() != null) {
      config.setTypePixKey(request.typePixKey());
    }
    if (request.deliveryFee() != null && request.deliveryFee().compareTo(BigDecimal.ZERO) >= 0) {
      config.setDeliveryFee(request.deliveryFee());
    }
    if (request.address() != null) {
      config.setAddress(request.address());
    }
    if (request.openTime() != null) {
      config.setOpenTime(request.openTime());
    }
    if (request.closeTime() != null) {
      config.setCloseTime(request.closeTime());
    }
    if (request.isManuallyClosed() != null) {
      config.setManuallyClosed(request.isManuallyClosed());
    }
    if (request.closedMessage() != null) {
      config.setClosedMessage(request.closedMessage());
    }
    if (request.estimatedTimeMinutes() != null) {
      config.setEstimatedTimeMinutes(request.estimatedTimeMinutes());
    }
    if (request.estimatedTimeText() != null) {
      config.setEstimatedTimeText(request.estimatedTimeText());
    }

    RestaurantConfig saved = repository.save(config);
    return RestaurantConfigResponse.fromEntity(saved);
  }
}
