package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Boolean active,
    Boolean allowsHalfAndHalf,
    Integer preparationTime,
    Long categoryId,
    String categoryName,
    Instant createAt,
    Set<IngredientResponse> ingredientsId,
    List<ProductSizePriceDto> sizes,
    String imageUrl
) {
  public ProductResponse(
      Long id,
      String name,
      String description,
      BigDecimal price,
      Boolean active,
      Boolean allowsHalfAndHalf,
      Integer preparationTime,
      Long categoryId,
      String categoryName,
      Instant createAt,
      Set<IngredientResponse> ingredientsId,
      List<ProductSizePriceDto> sizes
  ) {
    this(id, name, description, price, active, allowsHalfAndHalf, preparationTime, categoryId, categoryName, createAt, ingredientsId, sizes, null);
  }

  public static ProductResponse fromEntity(Product product) {
    List<ProductSizePriceDto> sizes = product.getSizePrices() != null
        ? product.getSizePrices()
            .stream()
            .map(ProductSizePriceDto::fromEntity)
            .toList()
        : List.of();
    Set<IngredientResponse> ingredientsId = product.getIngredient() != null
        ? product.getIngredient()
            .stream()
            .map(IngredientResponse::fromEntity)
            .collect(Collectors.toSet())
        : Set.of();

    Long categoryId = product.getCategoryProduct() != null ? product.getCategoryProduct().getId() : null;
    String categoryName = product.getCategoryProduct() != null ? product.getCategoryProduct().getName() : null;

    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getActive(),
        product.getAllowsHalfAndHalf(),
        product.getPreparationTimeInMinutes(),
        categoryId,
        categoryName,
        product.getCreateAt(),
        ingredientsId,
        sizes,
        product.getImageUrl()
    );
  }
}
