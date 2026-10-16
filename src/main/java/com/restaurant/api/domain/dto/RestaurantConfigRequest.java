package com.restaurant.api.domain.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record RestaurantConfigRequest(
    String restaurantName,
    String whatsappPhone,
    String pixKey,
    String typePixKey,
    BigDecimal deliveryFee,
    String address,
    LocalTime openTime,
    LocalTime closeTime,
    Boolean isManuallyClosed,
    String closedMessage,
    Integer estimatedTimeMinutes,
    String estimatedTimeText,
    java.util.List<String> openDays
) {
}
