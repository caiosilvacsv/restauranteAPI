package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.OrderType;
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
  private RestaurantTables table;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_type", nullable = false)
  private OrderType orderType = OrderType.DINE_IN;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "order_code", length = 10)
  private String orderCode;

  @Column(name = "client_order_id", length = 64)
  private String clientOrderId;

  @Column(name = "customer_name", length = 150)
  private String customerName;

  @Column(name = "customer_phone", length = 30)
  private String customerPhone;

  @Embedded
  private DeliveryAddress deliveryAddress;

  @Column(name = "cash_change_for")
  private java.math.BigDecimal cashChangeFor;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method")
  private com.restaurant.api.domain.enums.PaymentMethod paymentMethod;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "estimated_minutes")
  private Integer estimatedMinutes;

  @Column(name = "estimated_delivery_at")
  private Instant estimatedDeliveryAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    openedAt = Instant.now();
    if (updatedAt == null) {
      updatedAt = openedAt;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
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
    this.setTable(table);
    this.setNotes(notes);
  }
}
