package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.PizzaSize;

public record PizzaSizeResponse(
    Long id,
    String name,
    String shortName,
    Integer slices,
    Integer maxFlavors,
    Boolean active
) {
  public static PizzaSizeResponse fromEntity(PizzaSize size) {
    return new PizzaSizeResponse(
        size.getId(),
        size.getName(),
        size.getShortName(),
        size.getSlices(),
        size.getMaxFlavors(),
        size.getActive()
    );
  }
}
