package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.CategoryProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryProductRepository extends JpaRepository<CategoryProduct, Long> {
  List<CategoryProduct> findByActiveTrue();
  Page<CategoryProduct> findByActiveTrue(Pageable pageable);
  boolean existsByNameIgnoreCase(String name);
}
