package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.RestaurantTables;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTables, Long> {
}
