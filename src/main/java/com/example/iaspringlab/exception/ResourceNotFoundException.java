package com.example.iaspringlab.exception;

/**
 * Ressource métier introuvable.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
