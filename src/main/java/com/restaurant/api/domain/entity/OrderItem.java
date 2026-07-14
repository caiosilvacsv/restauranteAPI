package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.StatusOrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "OrderItems")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price",nullable = false)
  private BigDecimal unitPrice;

  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusOrderItem status = StatusOrderItem.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id",  nullable = false)
  private Order orderID;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product productID;

  @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemModification> modifications = new ArrayList<>();

}
