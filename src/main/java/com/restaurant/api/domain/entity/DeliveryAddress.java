package com.restaurant.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DeliveryAddress {

  @Column(name = "street", length = 150)
  private String street;

  @Column(name = "number", length = 20)
  private String number;

  @Column(name = "neighborhood", length = 100)
  private String neighborhood;

  @Column(name = "complement", length = 100)
  private String complement;

  @Column(name = "reference_point")
  private String referencePoint;
}
