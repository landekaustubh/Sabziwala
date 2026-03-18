package com.velocity.sabziwala.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when order status transition is invalid (e.g., DELIVERED → PENDING) */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
