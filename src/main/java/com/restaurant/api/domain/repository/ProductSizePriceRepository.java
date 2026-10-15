package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.ProductSizePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizePriceRepository extends JpaRepository<ProductSizePrice, Long> {
  List<ProductSizePrice> findByProductId(Long productId);
  Optional<ProductSizePrice> findByProductIdAndPizzaSizeId(Long productId, Long pizzaSizeId);
}
