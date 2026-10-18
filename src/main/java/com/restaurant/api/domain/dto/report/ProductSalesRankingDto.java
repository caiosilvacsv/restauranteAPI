package com.restaurant.api.domain.dto.report;

import java.math.BigDecimal;

public record ProductSalesRankingDto(
    Long productId,
    String productName,
    long quantitySold,
    BigDecimal totalAmount
) {}
