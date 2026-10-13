package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import com.example.iaspringlab.repository.OrderRepository;
import com.example.iaspringlab.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");

    private static Product product(String price) {
        return new Product("REF-1", "Produit", new BigDecimal(price));
    }

    @Test
    void createOrderRejectsUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CreateOrderRequest(99L, List.of(new OrderLineRequest(1L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrderRejectsEmptyLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));

        var request = new CreateOrderRequest(1L, List.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createOrderRejectsInactiveProduct() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        Product inactive = product("10.00");
        inactive.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 2)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderComputesTotalFromLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.createOrder(
                new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 3))));

        assertThat(response.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(response.total()).isEqualByComparingTo("30.00");
    }

    @Test
    void addLineIsRejectedWhenOrderIsNotDraft() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addLineAddsLineAndRecomputesTotal() {
        Order order = new Order(CLIENT);
        order.addLine(product("10.00"), 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product("20.00")));

        var response = orderService.addLine(5L, new AddOrderLineRequest(2L, 2));

        assertThat(response.lines()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("50.00");
    }

    @Test
    void confirmMovesDraftToConfirmed() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.confirm(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelMovesConfirmedToCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.cancel(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmIsRejectedOnCancelledOrder() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirm(3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getThrowsWhenOrderMissing() {
        when(orderRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrderRejectsUnknownProduct() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(7L)).thenReturn(Optional.empty());

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(7L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsNonPositiveQuantity() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 0)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("strictement positive");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderSumsAllLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product("2.50")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateOrderRequest(1L,
                List.of(new OrderLineRequest(1L, 3), new OrderLineRequest(2L, 4)));

        var response = orderService.createOrder(request);

        assertThat(response.lines()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("40.00");
    }

    @Test
    void addLineRejectsUnknownOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addLine(99L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addLineRejectsInactiveProduct() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        Product inactive = product("10.00");
        inactive.deactivate();
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(2L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactif");
        assertThat(order.getLines()).isEmpty();
    }

    @Test
    void addLineIsRejectedWhenOrderIsCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void confirmIsRejectedWhenOrderIsAlreadyConfirmed() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirm(3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelMovesDraftToCancelled() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.cancel(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelIsRejectedWhenOrderIsAlreadyCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(3L))
                .isInstanceOf(BusinessRuleException.class);
    }
}
