package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "category_products")
public class CategoryProduct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false,  unique = true)
  private String name;

  @Column(nullable = false)
  private Boolean active = true;

}
