package com.restaurant.api.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandle {

  @ExceptionHandler(BusinessRulesException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRulesException(BusinessRulesException e) {
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        e.getTitle(),
        List.of(e.getMessage())
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        HttpStatus.NOT_FOUND.value(),
        e.getTitle(),
        List.of(e.getMessage())
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(InvalidStateException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStateException(InvalidStateException e) {
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        HttpStatus.UNPROCESSABLE_CONTENT.value(),
        e.getTitle(),
        List.of(e.getMessage())
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
  }

  @ExceptionHandler(InvalidStateException.class)
  public ResponseEntity<ErrorResponse>  handleConflictException(ConflictException e) {
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        HttpStatus.CONFLICT.value(),
        e.getTitle(),
        List.of(e.getMessage())
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    ErrorResponse response = new ErrorResponse(
      Instant.now(),
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      e.getCause().toString(),
      List.of(e.getMessage())
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
