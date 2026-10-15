package com.restaurant.api.domain.enums;

public enum StatusPayment {
  PENDING,
  APPROVED,
  REJECTED,
  CANCELLED;

  public boolean hasFailed() {
    return this == CANCELLED || this == PENDING;
  }
}
