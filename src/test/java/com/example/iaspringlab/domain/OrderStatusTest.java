package com.example.iaspringlab.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void draftCanTransitionToConfirmedAndCancelled() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void draftCannotTransitionToDraft() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.DRAFT)).isFalse();
    }

    @Test
    void confirmedCanOnlyTransitionToCancelled() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DRAFT)).isFalse();
    }

    @Test
    void cancelledIsTerminal() {
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DRAFT)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void nullTargetIsRejected() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(null)).isFalse();
    }
}
