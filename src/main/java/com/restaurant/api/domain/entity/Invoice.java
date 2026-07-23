package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 *
 * In our system, the Invoice represents the bill closure.
 * When a customer requests to close their table's tab,
 * the system calculates all subtotals and fees,
 * creates an Invoice record, and prepares the order for payment.
 *
**/
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

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  @PrePersist
  public void prePersist() {
    this.closedAt = Instant.now();
  }
}
