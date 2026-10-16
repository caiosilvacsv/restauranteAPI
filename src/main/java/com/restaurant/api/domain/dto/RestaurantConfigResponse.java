package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.RestaurantConfig;

import java.math.BigDecimal;
import java.time.LocalTime;

public record RestaurantConfigResponse(
    Long id,
    String restaurantName,
    String whatsappPhone,
    String pixKey,
    String typePixKey,
    BigDecimal deliveryFee,
    String address,
    LocalTime openTime,
    LocalTime closeTime,
    boolean isManuallyClosed,
    String closedMessage,
    boolean isOpen,
    Integer estimatedTimeMinutes,
    String estimatedTimeText,
    java.util.List<String> openDays
) {
  public static RestaurantConfigResponse fromEntity(RestaurantConfig config) {
    if (config == null) return null;
    return new RestaurantConfigResponse(
        config.getId(),
        config.getRestaurantName(),
        config.getWhatsappPhone(),
        config.getPixKey(),
        config.getTypePixKey(),
        config.getDeliveryFee(),
        config.getAddress(),
        config.getOpenTime(),
        config.getCloseTime(),
        config.isManuallyClosed(),
        config.getClosedMessage(),
        config.isCurrentlyOpen(),
        config.getEstimatedTimeMinutes(),
        config.getEstimatedTimeText(),
        config.getOpenDaysList()
    );
  }
}
