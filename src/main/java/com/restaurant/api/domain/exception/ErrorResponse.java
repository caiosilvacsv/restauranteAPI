package com.restaurant.api.domain.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    Integer status,
    String error,
    List<String> messages
) {
}
