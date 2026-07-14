package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private BigDecimal subtotal;

  @Column(name = "service_fee", nullable = false)
  private BigDecimal serviceFee;

  @Column(nullable = false)
  private BigDecimal discount;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "closed_at", nullable = false)
  private Instant closedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order orderID;

  @PrePersist
  public void prePersist() {
    this.closedAt = Instant.now();
  }
}
