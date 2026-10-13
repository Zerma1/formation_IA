package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.AbstractIntegrationTest;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.DuplicateResourceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

@EnabledIfDockerAvailable
class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    private ClientResponse newClient() {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        return clientService.create(new CreateClientRequest("Ada", "Lovelace", email));
    }

    private ProductResponse newProduct(String price) {
        String reference = "REF-" + UUID.randomUUID();
        return productService.create(new CreateProductRequest(reference, "Produit", new BigDecimal(price)));
    }

    @Test
    void fullOrderLifecycle() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("12.50");

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 3))));

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.total()).isEqualByComparingTo("37.50");
        assertThat(order.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.quantity()).isEqualTo(3);
                    assertThat(line.unitPrice()).isEqualByComparingTo("12.50");
                    assertThat(line.lineTotal()).isEqualByComparingTo("37.50");
                });

        OrderResponse withExtraLine = orderService.addLine(order.id(), new AddOrderLineRequest(product.id(), 1));
        assertThat(withExtraLine.total()).isEqualByComparingTo("50.00");

        OrderResponse confirmed = orderService.confirm(order.id());
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> orderService.addLine(order.id(), new AddOrderLineRequest(product.id(), 1)))
                .isInstanceOf(BusinessRuleException.class);

        OrderResponse cancelled = orderService.cancel(order.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(orderService.get(order.id()).total()).isEqualByComparingTo("50.00");
    }

    @Test
    void inactiveProductCannotBeAddedToAnOrder() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("9.99");
        productService.updateStatus(product.id(), new UpdateProductStatusRequest(false));

        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void duplicateClientEmailIsRejected() {
        ClientResponse client = newClient();

        assertThatThrownBy(() -> clientService.create(
                new CreateClientRequest("Grace", "Hopper", client.email())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void unitPriceIsCapturedFromTheProductWhenTheLineIsCreated() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("100.00");

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 2))));

        assertThat(order.lines()).singleElement()
                .satisfies(line -> assertThat(line.unitPrice()).isEqualByComparingTo("100.00"));
        assertThat(order.total()).isEqualByComparingTo("200.00");
    }

    @Test
    void confirmingTwiceIsRejected() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));
        orderService.confirm(order.id());

        assertThatThrownBy(() -> orderService.confirm(order.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void draftOrderCanBeCancelledDirectly() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));

        assertThat(orderService.cancel(order.id()).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void responseExposesClientFullName() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));

        assertThat(order.clientName()).isEqualTo("Ada Lovelace");
    }
}
