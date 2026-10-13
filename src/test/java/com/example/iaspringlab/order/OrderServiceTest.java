package com.example.iaspringlab.order;

import com.example.iaspringlab.customer.Customer;
import com.example.iaspringlab.customer.CustomerRepository;
import com.example.iaspringlab.exception.InactiveProductException;
import com.example.iaspringlab.exception.InvalidOrderStatusTransitionException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.product.Product;
import com.example.iaspringlab.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        customer = new Customer("Ada", "Lovelace", "ada@example.com");
        ReflectionTestUtils.setField(customer, "id", 1L);

        activeProduct = new Product("REF-1", "Widget", new BigDecimal("10.00"), true);
        ReflectionTestUtils.setField(activeProduct, "id", 10L);

        inactiveProduct = new Product("REF-2", "Gadget", new BigDecimal("5.00"), false);
        ReflectionTestUtils.setField(inactiveProduct, "id", 20L);
    }

    @Test
    void create_computesTotalAsSumOfQuantityTimesSnapshotPrice() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(activeProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, 3)));

        OrderResponse response = orderService.create(request);

        assertThat(response.total()).isEqualByComparingTo("30.00");
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    void create_sumsMultipleLines() {
        Product second = new Product("REF-3", "Gizmo", new BigDecimal("2.50"), true);
        ReflectionTestUtils.setField(second, "id", 11L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(activeProduct, second));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(1L, List.of(
                new OrderLineRequest(10L, 2),
                new OrderLineRequest(11L, 4)
        ));

        OrderResponse response = orderService.create(request);

        assertThat(response.total()).isEqualByComparingTo("30.00");
    }

    @Test
    void create_throwsResourceNotFoundException_whenCustomerMissing() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsResourceNotFoundException_whenProductMissing() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of());
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsInactiveProductException_whenProductIsInactive() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(20L))).thenReturn(List.of(inactiveProduct));
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(20L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(InactiveProductException.class);
    }

    @Test
    void changeStatus_confirmsDraftOrder() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(activeProduct, 1, activeProduct.getPrice()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.changeStatus(1L, new OrderStatusUpdateRequest(OrderStatus.CONFIRMED));

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void changeStatus_cancelsConfirmedOrder() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(activeProduct, 1, activeProduct.getPrice()));
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.changeStatus(1L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void changeStatus_throwsInvalidOrderStatusTransitionException_whenTransitionNotAllowed() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(activeProduct, 1, activeProduct.getPrice()));
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, new OrderStatusUpdateRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void changeStatus_throwsResourceNotFoundException_whenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.changeStatus(99L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsOnlyOrdersForGivenCustomer_whenCustomerIdProvided() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(activeProduct, 1, activeProduct.getPrice()));
        when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getAll(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo(1L);
    }

    @Test
    void getAll_returnsEveryOrder_whenCustomerIdIsNull() {
        Order order = new Order(customer);
        order.addLine(new OrderLine(activeProduct, 1, activeProduct.getPrice()));
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getAll(null);

        assertThat(responses).hasSize(1);
        org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.never()).findByCustomerId(any());
    }
}
