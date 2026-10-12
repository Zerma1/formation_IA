package com.example.iaspringlab.exception;

/**
 * Violation d'une règle métier (transition de statut interdite, produit inactif, etc.).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
