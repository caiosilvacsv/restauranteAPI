package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "product_size_prices", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "pizza_size_id"})
})
public class ProductSizePrice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pizza_size_id", nullable = false)
  private PizzaSize pizzaSize;

  @Column(nullable = false)
  private BigDecimal price;
}
