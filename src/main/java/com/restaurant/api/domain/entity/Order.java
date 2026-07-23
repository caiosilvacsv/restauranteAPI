package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.StatusOrder;
import com.restaurant.api.domain.exception.InvalidStateException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "opened_at",  nullable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusOrder status =  StatusOrder.OPENED;

  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "table_id")
  private RestaurantTables tablesID;

  @PrePersist
  public void prePersist() {
    openedAt = Instant.now();
  }

  public void close(){
    this.setClosedAt(Instant.now());
    if(this.status.canTransitionTo(StatusOrder.CLOSED))
      this.setStatus(StatusOrder.CLOSED);
    else
      throw new InvalidStateException(
          "Order closed",
          "This order has already been closed or invalid status");
  }

  public void advanceStatus(){
    this.status = this.status.next();
  }

  public Order(){}

  public Order(RestaurantTables table, String notes){
    this.setTablesID(table);
    this.setNotes(notes);
  }
}
