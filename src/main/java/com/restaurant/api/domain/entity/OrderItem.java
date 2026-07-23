package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.StatusOrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

  @Column(name = "preparation_started_at")
  private Instant preparationStartedAt;

  @Column(name = "ready_at")
  private Instant readyAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

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
  private Set<OrderItemModification> modifications = new HashSet<>();

  public void next(){
    this.getStatus().next();
  }

  public void initPrepared(){
    this.setPreparationStartedAt(Instant.now());
  }

  public void isReady(){
    this.setReadyAt(Instant.now());
  }

  public void delivered(){
    this.setDeliveredAt(Instant.now());
  }
}
