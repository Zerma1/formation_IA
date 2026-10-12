package com.example.iaspringlab.domain;

/**
 * Statuts possibles d'une commande et transitions autorisées.
 *
 * <pre>
 * DRAFT      -> CONFIRMED
 * DRAFT      -> CANCELLED
 * CONFIRMED  -> CANCELLED
 * </pre>
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED;

    /**
     * Indique si la transition depuis le statut courant vers {@code target} est autorisée.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
