package com.example.iaspringlab.exception;

/**
 * Thrown when a resource would violate a uniqueness constraint. Mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
