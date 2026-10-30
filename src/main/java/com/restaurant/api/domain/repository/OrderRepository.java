package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.enums.StatusOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
  Optional<Order> findByClientOrderId(String clientOrderId);
  Optional<Order> findFirstByTableIdAndStatusNotIn(Long tableId, Collection<StatusOrder> statuses);
  Page<Order> findByUserIdOrderByOpenedAtDesc(Long userId, Pageable pageable);
  Page<Order> findByStatusOrderByOpenedAtDesc(StatusOrder status, Pageable pageable);
  java.util.List<Order> findByOpenedAtBetweenOrderByOpenedAtAsc(java.time.Instant start, java.time.Instant end);
}
