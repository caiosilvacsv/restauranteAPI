package com.restaurant.api.domain.exception;

import lombok.Getter;

@Getter
public class UserCredentials extends RuntimeException {
  private final String title;

  public UserCredentials(String title, String message) {
    super(message);
    this.title = title;
  }

  public  UserCredentials(String message) {
    super(message);
    this.title = "Not found.";
  }

  public UserCredentials() {
    super("Unauthorized User.");
    this.title = "Unauthorized user.";
  }
}
