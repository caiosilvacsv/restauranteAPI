package com.restaurant.api.domain.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {
  private final String title;

  public ConflictException(String title, String message) {
    super(message);
    this.title = title;
  }

  public  ConflictException(String message) {
    super(message);
    this.title = "Conflict in order.";
  }

  public ConflictException() {
    super("Conflict in order.");
    this.title = "Conflict in order.";
  }
}
