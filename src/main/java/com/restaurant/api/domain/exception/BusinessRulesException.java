package com.restaurant.api.domain.exception;

import lombok.Getter;

@Getter
public class BusinessRulesException extends RuntimeException {
  private final String title;

  public BusinessRulesException(String title, String message) {
    super(message);
    this.title = title;
  }

  public BusinessRulesException(String message) {
    super(message);
    this.title = "Error business rules";
  }

  public BusinessRulesException() {
    super("Error business rules");
    this.title = "Error business rules";
  }
}
