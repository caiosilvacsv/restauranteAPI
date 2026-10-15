package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
  Set<Ingredient> findAllByIdInAndActiveTrue(Set<Long> ids);
  boolean existsByName(String name);
  Page<Ingredient> findByActiveTrueOrderByNameAsc(Pageable pageable);
  Page<Ingredient> findByActiveFalseOrderByNameAsc(Pageable pageable);
}
