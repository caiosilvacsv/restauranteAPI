package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  @Column(name = "delivery_fee", nullable = false)
  private BigDecimal deliveryFee = BigDecimal.valueOf(2.00);

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

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
  private Set<Payment> payments = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.closedAt = Instant.now();
  }
}
