package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.Order;
import com.restaurant.api.domain.enums.StatusOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
  @EntityGraph(attributePaths = {"table", "user"})
  Optional<Order> findByClientOrderId(String clientOrderId);

  boolean existsByClientOrderId(String clientOrderId);

  Optional<Order> findFirstByTableIdAndStatusNotIn(Long tableId, Collection<StatusOrder> statuses);

  @EntityGraph(attributePaths = {"table", "user"})
  Page<Order> findByUserIdOrderByOpenedAtDesc(Long userId, Pageable pageable);

  @EntityGraph(attributePaths = {"table", "user"})
  Page<Order> findByStatusOrderByOpenedAtDesc(StatusOrder status, Pageable pageable);

  java.util.List<Order> findByOpenedAtBetweenOrderByOpenedAtAsc(java.time.Instant start, java.time.Instant end);

  @Override
  @EntityGraph(attributePaths = {"table", "user"})
  Page<Order> findAll(Specification<Order> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"table", "user"})
  Page<Order> findAll(Pageable pageable);
}
