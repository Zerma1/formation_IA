package com.example.iaspringlab.domain;

/**
 * Lifecycle of an order.
 *
 * <p>Only the following transitions are allowed (business rule):</p>
 * <pre>
 * DRAFT      -&gt; CONFIRMED
 * DRAFT      -&gt; CANCELLED
 * CONFIRMED  -&gt; CANCELLED
 * </pre>
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED;

    /**
     * @return {@code true} when moving from this status to {@code target} is allowed.
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
