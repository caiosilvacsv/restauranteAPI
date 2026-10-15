package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Page<Product> findByActiveTrue(Pageable pageable);
  Page<Product> findByActiveFalse(Pageable pageable);
}
