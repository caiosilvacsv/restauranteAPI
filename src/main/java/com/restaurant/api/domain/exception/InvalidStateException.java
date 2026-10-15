package com.restaurant.api.domain.exception;

import lombok.Getter;

@Getter
public class InvalidStateException extends RuntimeException {
  private final String title;

  public InvalidStateException(String title, String message) {
    super(message);
    this.title = title;
  }

  public  InvalidStateException(String message) {
    super(message);
    this.title = "Invalid State.";
  }

  public InvalidStateException() {
    super("Invalid State.");
    this.title = "Invalid State.";
  }
}
