package com.velocity.sabziwala.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a CUSTOMER tries to access another customer's order.
 * ADMIN can access any order, but CUSTOMER can only access their own.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedOrderAccessException extends RuntimeException {
    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}
