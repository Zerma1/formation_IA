package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    private Long clientId;
    private Long activeProductId;
    private Long inactiveProductId;

    @BeforeEach
    void setUp() {
        ClientResponse client = clientService.create(
                new CreateClientRequest("John", "Doe", "john+" + System.nanoTime() + "@example.com"));
        clientId = client.id();

        ProductResponse active = productService.create(
                new CreateProductRequest("REF-ACTIVE-" + System.nanoTime(), "Produit actif", BigDecimal.valueOf(10.00)));
        activeProductId = active.id();

        ProductResponse inactive = productService.create(
                new CreateProductRequest("REF-INACTIVE-" + System.nanoTime(), "Produit inactif", BigDecimal.valueOf(20.00)));
        inactiveProductId = inactive.id();
        productService.updateStatus(inactiveProductId, false);
    }

    @Test
    void createOrderComputesTotalAndSnapshotsPrice() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 3))));

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.total()).isEqualByComparingTo("30.00");
        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().get(0).unitPrice()).isEqualByComparingTo("10.00");
        assertThat(order.lines().get(0).lineTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void createOrderRejectsInactiveProduct() {
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(inactiveProductId, 1)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactif");
    }

    @Test
    void createOrderRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 0)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createOrderRejectsUnknownClient() {
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                999_999L, List.of(new OrderLineRequest(activeProductId, 1)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addLineOnlyAllowedOnDraft() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 1))));
        orderService.confirm(order.id());

        assertThatThrownBy(() -> orderService.addLine(order.id(),
                new AddOrderLineRequest(activeProductId, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addLineRecalculatesTotal() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 1))));

        OrderResponse updated = orderService.addLine(order.id(), new AddOrderLineRequest(activeProductId, 2));

        assertThat(updated.total()).isEqualByComparingTo("30.00");
        assertThat(updated.lines()).hasSize(2);
    }

    @Test
    void confirmThenCancelSucceeds() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 1))));

        assertThat(orderService.confirm(order.id()).status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.cancel(order.id()).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelledOrderCannotBeConfirmed() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 1))));
        orderService.cancel(order.id());

        assertThatThrownBy(() -> orderService.confirm(order.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unitPriceIsNotImpactedByLaterProductPriceChange() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                clientId, List.of(new OrderLineRequest(activeProductId, 2))));

        OrderResponse refreshed = orderService.get(order.id());
        assertThat(refreshed.lines().get(0).unitPrice()).isEqualByComparingTo("10.00");
        assertThat(refreshed.total()).isEqualByComparingTo("20.00");
    }
}
