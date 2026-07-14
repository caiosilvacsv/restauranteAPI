package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.CategoryProduct;
import com.restaurant.api.domain.entity.Product;
import java.math.BigDecimal;
import java.util.Set;

public record ProductRequest(
    Long categoryId,
    String name,
    String description,
    BigDecimal price,
    Boolean active,
    Integer preparationTimeInMinutes,
    Set<Long> ingredientsId
) {

  public Product toEntity(CategoryProduct category) {
    Product product = new Product();
    toPopulate(product, category);
    return product;
  }

  public void toPopulate(Product product, CategoryProduct category) {
    product.setName(name);
    product.setDescription(description);
    product.setPrice(price != null && price.compareTo(BigDecimal.ZERO) > 0 ? price : BigDecimal.ONE);
    product.setActive(active != null ? active : true);
    product.setPreparationTimeInMinutes(
        preparationTimeInMinutes != null && preparationTimeInMinutes > 0 ? preparationTimeInMinutes : 60);
  }
}
