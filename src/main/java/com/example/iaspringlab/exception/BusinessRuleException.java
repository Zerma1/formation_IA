package com.example.iaspringlab.exception;

/**
 * Thrown when an operation violates a documented business rule. Mapped to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
