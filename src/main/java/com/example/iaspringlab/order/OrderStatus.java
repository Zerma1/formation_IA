package com.example.iaspringlab.order;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    DRAFT,
    CONFIRMED,
    CANCELLED;

    private static final Set<OrderStatus> NO_TRANSITIONS = EnumSet.noneOf(OrderStatus.class);

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case DRAFT -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> NO_TRANSITIONS.contains(target);
        };
    }
}
