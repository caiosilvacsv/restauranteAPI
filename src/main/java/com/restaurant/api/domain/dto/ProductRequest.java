package com.restaurant.api.domain.dto;

import com.restaurant.api.domain.entity.CategoryProduct;
import com.restaurant.api.domain.entity.Ingredient;
import com.restaurant.api.domain.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ProductRequest(
    Long categoryId,
    String name,
    String description,
    BigDecimal price,
    Boolean active,
    Boolean allowsHalfAndHalf,
    Integer preparationTimeInMinutes,
    Set<Long> ingredientsId,
    List<ProductSizePriceDto> sizes
) {

  public Product toEntity(CategoryProduct category, Set<Ingredient> ingredients) {
    Product product = new Product();
    toPopulate(product, category, ingredients);
    return product;
  }

  public void toPopulate(
      Product product,
      CategoryProduct category,
      Set<Ingredient> ingredients) {
    product.setCategoryProduct(category);
    product.setName(name);
    product.setDescription(description);
    product.setPrice(price != null && price.compareTo(BigDecimal.ZERO) > 0 ? price : BigDecimal.ONE);
    product.setActive(active != null ? active : true);
    product.setAllowsHalfAndHalf(allowsHalfAndHalf != null ? allowsHalfAndHalf : false);
    product.setPreparationTimeInMinutes(
        preparationTimeInMinutes != null && preparationTimeInMinutes > 0 ? preparationTimeInMinutes : 60);

    product.getIngredient().clear();
    if(ingredients != null && !ingredients.isEmpty())
      product.getIngredient().addAll(ingredients);
  }
}
