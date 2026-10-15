package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.PizzaSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PizzaSizeRepository extends JpaRepository<PizzaSize, Long> {
  List<PizzaSize> findByActiveTrue();
  Optional<PizzaSize> findByShortNameIgnoreCase(String shortName);
}
