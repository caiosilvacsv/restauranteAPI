package com.restaurant.api.domain.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
  private final String title;

  public NotFoundException(String title, String message) {
    super(message);
    this.title = title;
  }

  public  NotFoundException(String message) {
    super(message);
    this.title = "Not found.";
  }

  public NotFoundException() {
    super("Not found.");
    this.title = "Not found.";
  }
}
