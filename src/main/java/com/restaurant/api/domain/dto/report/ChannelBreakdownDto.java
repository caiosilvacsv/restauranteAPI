package com.restaurant.api.domain.dto.report;

import com.restaurant.api.domain.enums.OrderType;

import java.math.BigDecimal;

public record ChannelBreakdownDto(
    OrderType orderType,
    String description,
    BigDecimal totalAmount,
    long count,
    BigDecimal percentage
) {}
