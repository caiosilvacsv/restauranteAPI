package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.PaymentMethod;
import com.restaurant.api.domain.enums.StatusPayment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentMethod method;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private StatusPayment status =  StatusPayment.PENDING;

  @Column(name = "external_transaction_code", nullable = false)
  private String externalTransactionCode;

  @Column(name = "payment_date")
  private Instant paymentDate;

  @Column(name = "create_at", nullable = false)
  private Instant createAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  @PrePersist
  public void prePersist() {
    this.createAt = Instant.now();
  }

}
