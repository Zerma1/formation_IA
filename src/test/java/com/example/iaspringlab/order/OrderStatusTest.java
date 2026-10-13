package com.example.iaspringlab.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed = {2}")
    @CsvSource({
            "DRAFT,     CONFIRMED,  true",
            "DRAFT,     CANCELLED,  true",
            "DRAFT,     DRAFT,      false",
            "CONFIRMED, CANCELLED,  true",
            "CONFIRMED, DRAFT,      false",
            "CONFIRMED, CONFIRMED,  false",
            "CANCELLED, DRAFT,      false",
            "CANCELLED, CONFIRMED,  false",
            "CANCELLED, CANCELLED,  false"
    })
    void canTransitionTo_followsBusinessRuleMatrix(OrderStatus from, OrderStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}
