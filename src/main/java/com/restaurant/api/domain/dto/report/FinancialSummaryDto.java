package com.restaurant.api.domain.dto.report;

import java.math.BigDecimal;

public record FinancialSummaryDto(
    BigDecimal grossTotal,
    BigDecimal deliveryFeeTotal,
    BigDecimal netTotal,
    BigDecimal averageTicket
) {}
