package com.restaurant.api.domain.entity;

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
@Table(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(name = "preparation_time_minutes")
  private Integer preparationTimeInMinutes;

  @Column(name = "created_at", nullable = false)
  private Instant createAt;

  @Column(name = "updated_at")
  private Instant updateAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private CategoryProduct categoryProduct;

  @ManyToMany
  @JoinTable(
      name = "product_ingredients",
      joinColumns = @JoinColumn(name = "product_id"),
      inverseJoinColumns = @JoinColumn(name = "ingredient_id")
  )
  private Set<Ingredient> ingredient = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.createAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updateAt = Instant.now();
  }
}
