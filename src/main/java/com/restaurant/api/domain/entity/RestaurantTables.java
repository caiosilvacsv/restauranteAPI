package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.StatusTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="restaurant_tables")
public class RestaurantTables {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  ///table identification number
  @Column(nullable = false)
  private Integer number;

  private String description;

  @Column(nullable = false)
  private Integer capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusTable status = StatusTable.AVAILABLE;

  public void reserverTable(){
    this.status = StatusTable.RESERVED;
  }

  public void occupyTable(){
    this.status = StatusTable.OCCUPIED;
  }

  public void availableTable(){
    this.status = StatusTable.AVAILABLE;
  }

}

