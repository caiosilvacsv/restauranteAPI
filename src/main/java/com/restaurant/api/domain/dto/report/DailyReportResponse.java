package com.restaurant.api.domain.dto.report;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
    LocalDate businessDate,
    String formattedDate,
    Instant shiftStart,
    Instant shiftEnd,
    FinancialSummaryDto financial,
    List<PaymentMethodBreakdownDto> paymentMethods,
    List<ChannelBreakdownDto> channels,
    OperationsSummaryDto operations,
    List<ProductSalesRankingDto> topProducts
) {}
