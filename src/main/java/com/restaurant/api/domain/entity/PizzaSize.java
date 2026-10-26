package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "pizza_sizes")
public class PizzaSize {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "short_name", nullable = false, unique = true, length = 10)
  private String shortName;

  @Column(name = "slices")
  private Integer slices;

  @Column(name = "max_flavors", nullable = false)
  private Integer maxFlavors = 1;

  @Column(nullable = false)
  private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = Instant.now();
  }
}
