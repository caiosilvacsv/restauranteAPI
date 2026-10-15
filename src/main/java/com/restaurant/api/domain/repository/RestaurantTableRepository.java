package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.RestaurantTables;
import com.restaurant.api.domain.enums.StatusTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTables, Long> {
  List<RestaurantTables> findByStatusOrderByNumberAsc(StatusTable status);
  List<RestaurantTables> findAllByOrderByNumberAsc();
  Optional<RestaurantTables> findByNumber(Integer number);
  boolean existsByNumber(Integer number);
}
