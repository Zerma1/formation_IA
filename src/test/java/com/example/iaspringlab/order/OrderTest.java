package com.example.iaspringlab.order;

import com.example.iaspringlab.customer.Customer;
import com.example.iaspringlab.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavior under test: Order/OrderLine domain invariants that the business rules
 * rely on but that OrderServiceTest only exercises indirectly through mocks:
 * - total() = sum(quantity x unitPrice) across all lines, including the empty case.
 * - addLine() wires the bidirectional association.
 * - the unit price is a true snapshot: once a line is created, later changes to the
 *   product's price must NOT retroactively change the line's unitPrice or the order's total.
 */
class OrderTest {

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer("Ada", "Lovelace", "ada@example.com");
        product = new Product("REF-1", "Widget", new BigDecimal("10.00"), true);
    }

    @Test
    void total_isZero_whenOrderHasNoLines() {
        Order order = new Order(customer);

        assertThat(order.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void total_sumsQuantityTimesUnitPrice_acrossAllLines() {
        Order order = new Order(customer);
        Product second = new Product("REF-2", "Gizmo", new BigDecimal("2.50"), true);

        order.addLine(new OrderLine(product, 3, product.getPrice()));
        order.addLine(new OrderLine(second, 4, second.getPrice()));

        assertThat(order.total()).isEqualByComparingTo("40.00");
    }

    @Test
    void addLine_setsBackReferenceToOwningOrder() {
        Order order = new Order(customer);
        OrderLine line = new OrderLine(product, 1, product.getPrice());

        order.addLine(line);

        assertThat(line.getOrder()).isSameAs(order);
        assertThat(order.getLines()).containsExactly(line);
    }

    @Test
    void lineUnitPrice_isSnapshotAtCreationTime_andIgnoresLaterProductPriceChanges() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(product, 2, product.getPrice()));

        // The product's price changes after the order line was created (e.g. a price update).
        product.setPrice(new BigDecimal("99.99"));

        assertThat(order.getLines().get(0).getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(order.total()).isEqualByComparingTo("20.00");
    }

    @Test
    void newOrder_startsInDraftStatus() {
        Order order = new Order(customer);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }
}
