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

  @Query("""
    SELECT i
    FROM OrderItem i
    JOIN FETCH i.product
    LEFT JOIN FETCH i.secondProduct
    LEFT JOIN FETCH i.pizzaSize
    WHERE i.order.id IN :orderIds
    AND (i.status != :excludedStatus OR i.status IS NULL)
""")
  List<OrderItem> findValidItemsByOrderIds(
      @org.springframework.data.repository.query.Param("orderIds") java.util.Collection<Long> orderIds,
      @org.springframework.data.repository.query.Param("excludedStatus") StatusOrderItem excludedStatus
  );

  @Query("""
    SELECT DISTINCT i FROM OrderItem i
    JOIN FETCH i.order
    JOIN FETCH i.product
    LEFT JOIN FETCH i.secondProduct
    LEFT JOIN FETCH i.pizzaSize
    LEFT JOIN FETCH i.modifications m
    LEFT JOIN FETCH m.ingredient
    WHERE i.order.id IN :orderIds
    ORDER BY i.id ASC
  """)
  List<OrderItem> findByOrderIdInWithDetails(@org.springframework.data.repository.query.Param("orderIds") java.util.Collection<Long> orderIds);
}
