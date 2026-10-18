package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.ModificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_item_modifications")
public class OrderItemModification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", nullable = false)
  private OrderItem orderItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private Ingredient ingredient;

  @Column(nullable = false)
  private Integer quantity = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ModificationType modificationType;

  @Column(name = "extra_price", nullable = false)
  private BigDecimal extraPrice;
}
