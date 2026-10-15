package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.ProductSizePrice;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductSizePriceDto(
    @NotNull(message = "O ID do tamanho é obrigatório")
    Long sizeId,

    String sizeName,

    String sizeShortName,

    Integer slices,

    Integer maxFlavors,

    @NotNull(message = "O preço é obrigatório")
    @PositiveOrZero(message = "O preço deve ser maior ou igual a zero")
    BigDecimal price
) {
  public static ProductSizePriceDto fromEntity(ProductSizePrice psp) {
    return new ProductSizePriceDto(
        psp.getPizzaSize().getId(),
        psp.getPizzaSize().getName(),
        psp.getPizzaSize().getShortName(),
        psp.getPizzaSize().getSlices(),
        psp.getPizzaSize().getMaxFlavors(),
        psp.getPrice()
    );
  }
}
