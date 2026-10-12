package com.example.iaspringlab.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderDomainTest {

    private final Client client = new Client("John", "Doe", "john@example.com");

    @Test
    void addLineRejectsNonPositiveQuantity() {
        Order order = new Order(client);
        Product product = new Product("REF-1", "Produit", BigDecimal.valueOf(10));

        assertThatThrownBy(() -> order.addLine(product, 0))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> order.addLine(product, -3))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addLineSnapshotsUnitPriceAndComputesTotals() {
        Order order = new Order(client);
        Product product = new Product("REF-1", "Produit", BigDecimal.valueOf(10.50));

        OrderLine line = order.addLine(product, 3);

        assertThat(line.getUnitPrice()).isEqualByComparingTo("10.50");
        assertThat(line.getLineTotal()).isEqualByComparingTo("31.50");
        assertThat(order.getTotal()).isEqualByComparingTo("31.50");
    }

    @Test
    void totalIsSumOfLines() {
        Order order = new Order(client);
        order.addLine(new Product("REF-1", "A", BigDecimal.valueOf(10)), 2);
        order.addLine(new Product("REF-2", "B", BigDecimal.valueOf(5)), 4);

        assertThat(order.getTotal()).isEqualByComparingTo("40");
    }

    @Test
    void cannotConfirmOrderWithoutLine() {
        Order order = new Order(client);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotCancelCancelledOrder() {
        Order order = new Order(client);
        order.addLine(new Product("REF-1", "A", BigDecimal.valueOf(10)), 1);
        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotGoBackFromConfirmedToDraft() {
        Order order = new Order(client);
        order.addLine(new Product("REF-1", "A", BigDecimal.valueOf(10)), 1);
        order.transitionTo(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DRAFT))
                .isInstanceOf(BusinessRuleException.class);
    }
}
