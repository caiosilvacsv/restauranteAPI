package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.DeliveryAddress;

public record DeliveryAddressDto(
    String street,
    String number,
    String neighborhood,
    String complement,
    String referencePoint
) {
  public DeliveryAddress toEntity() {
    return new DeliveryAddress(street, number, neighborhood, complement, referencePoint);
  }

  public static DeliveryAddressDto fromEntity(DeliveryAddress address) {
    if (address == null) return null;
    return new DeliveryAddressDto(
        address.getStreet(),
        address.getNumber(),
        address.getNeighborhood(),
        address.getComplement(),
        address.getReferencePoint()
    );
  }
}
