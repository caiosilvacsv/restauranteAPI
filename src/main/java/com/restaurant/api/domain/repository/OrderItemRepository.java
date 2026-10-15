package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.OrderItem;
import com.restaurant.api.domain.enums.StatusOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderId(Long orderId);
  List<OrderItem> findByStatusOrderByIdAsc(StatusOrderItem statusOrder);
  List<OrderItem> findByOrderIdAndStatusNot(Long orderId, StatusOrderItem statusOrder);

  @Query("""
    SELECT i
    FROM OrderItem i
    JOIN FETCH i.product
    JOIN FETCH i.order p
    JOIN FETCH p.table
    WHERE i.status = :status
    ORDER BY i.id
""")
  List<OrderItem> findItemsWithProductAndOrder(StatusOrderItem status);
}
