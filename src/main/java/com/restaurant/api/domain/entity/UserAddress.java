package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_addresses")
public class UserAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(length = 50)
  private String nickname;

  @Column(nullable = false, length = 150)
  private String street;

  @Column(nullable = false, length = 20)
  private String number;

  @Column(nullable = false, length = 100)
  private String neighborhood;

  @Column(length = 100)
  private String complement;

  @Column(name = "reference_point")
  private String referencePoint;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  public DeliveryAddress toDeliveryAddress() {
    return new DeliveryAddress(
        this.street,
        this.number,
        this.neighborhood,
        this.complement,
        this.referencePoint
    );
  }
}
