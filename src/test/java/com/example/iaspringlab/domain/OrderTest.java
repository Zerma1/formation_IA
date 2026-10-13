package com.example.iaspringlab.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");

    private static Product product(String reference, String price) {
        return new Product(reference, "Produit " + reference, new BigDecimal(price));
    }

    @Test
    void newOrderIsDraftWithZeroTotalAndNoLine() {
        Order order = new Order(CLIENT);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getLines()).isEmpty();
    }

    @Test
    void addLineComputesTotal() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "12.50"), 3);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo("37.50");
    }

    @Test
    void addLineSnapshotsProductUnitPrice() {
        Product product = product("A", "12.50");
        Order order = new Order(CLIENT);

        order.addLine(product, 3);

        OrderLine line = order.getLines().getFirst();
        assertThat(line.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(line.getLineTotal()).isEqualByComparingTo("37.50");
    }

    @Test
    void totalIsTheSumOfAllLines() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "10.00"), 2);
        order.addLine(product("B", "5.50"), 4);

        assertThat(order.getTotal()).isEqualByComparingTo("42.00");
    }

    @Test
    void addLineRejectsNonPositiveQuantity() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.addLine(product("A", "10.00"), 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void draftCanBeConfirmed() {
        Order order = new Order(CLIENT);

        order.transitionTo(OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void illegalTransitionIsRejected() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void addLineRejectsNonPositiveQuantityWithoutMutatingOrder(int quantity) {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.addLine(product("A", "10.00"), quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("strictement positive");
        assertThat(order.getLines()).isEmpty();
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void draftCanBeCancelledDirectly() {
        Order order = new Order(CLIENT);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmedCanBeCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transitionToSameStatusIsRejected() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DRAFT))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void transitionToNullIsRejected() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.transitionTo(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void totalUsesExactDecimalArithmetic() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "0.10"), 3);
        order.addLine(product("B", "19.99"), 3);

        assertThat(order.getTotal()).isEqualByComparingTo("60.27");
    }
}
