package com.restaurant.api.domain.exception;

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
    this.title = "Error business rules";
    super("Error business rules");
  }

  public String getTitle() {
    return title;
  }
}
