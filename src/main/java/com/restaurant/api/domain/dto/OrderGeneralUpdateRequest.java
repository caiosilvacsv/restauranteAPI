package com.restaurant.api.domain.dto;

public record OrderGeneralUpdateRequest(
    String note,
    Long tableId,
    Long addressId,
    DeliveryAddressDto deliveryAddress
) {}
