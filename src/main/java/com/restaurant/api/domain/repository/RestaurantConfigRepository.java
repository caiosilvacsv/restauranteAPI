package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.RestaurantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantConfigRepository extends JpaRepository<RestaurantConfig, Long> {
}
