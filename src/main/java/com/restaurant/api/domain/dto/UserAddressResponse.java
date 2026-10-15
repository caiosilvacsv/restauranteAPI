package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.UserAddress;

public record UserAddressResponse(
    Long id,
    String nickname,
    String street,
    String number,
    String neighborhood,
    String complement,
    String referencePoint,
    Boolean isDefault,
    Boolean active
) {
  public static UserAddressResponse fromEntity(UserAddress address) {
    return new UserAddressResponse(
        address.getId(),
        address.getNickname(),
        address.getStreet(),
        address.getNumber(),
        address.getNeighborhood(),
        address.getComplement(),
        address.getReferencePoint(),
        address.getIsDefault(),
        address.getActive()
    );
  }
}
