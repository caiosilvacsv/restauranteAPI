package com.restaurant.api.domain.dto.report;

public record OperationsSummaryDto(
    long totalOrdersCreated,
    long completedOrders,
    long cancelledOrders
) {}
