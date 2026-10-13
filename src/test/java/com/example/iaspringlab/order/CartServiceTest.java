package com.example.iaspringlab.order;

import com.example.iaspringlab.customer.Customer;
import com.example.iaspringlab.customer.CustomerRepository;
import com.example.iaspringlab.exception.InactiveProductException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

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
    void addProduct_createsDraftOrder_whenCustomerHasNoCart() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse cart = cartService.addProduct(1L, 10L, 2);

        assertThat(cart.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(cart.lines()).hasSize(1);
        assertThat(cart.total()).isEqualByComparingTo("20.00");
    }

    @Test
    void addProduct_increasesQuantity_whenProductAlreadyInCart() {
        Order cart = cartWithLine(activeProduct, 2);
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct));
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.of(cart));
        when(orderRepository.save(cart)).thenReturn(cart);

        OrderResponse response = cartService.addProduct(1L, 10L, 3);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).quantity()).isEqualTo(5);
        assertThat(response.total()).isEqualByComparingTo("50.00");
    }

    @Test
    void addProduct_rejectsInactiveProduct() {
        when(productRepository.findById(20L)).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> cartService.addProduct(1L, 20L, 1))
                .isInstanceOf(InactiveProductException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void addProduct_rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> cartService.addProduct(1L, 10L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateQuantity_replacesLineQuantity() {
        Order cart = cartWithLine(activeProduct, 2);
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.of(cart));

        OrderResponse response = cartService.updateQuantity(1L, 100L, 7);

        assertThat(response.lines().get(0).quantity()).isEqualTo(7);
    }

    @Test
    void removeLine_deletesCart_whenLastLineRemoved() {
        Order cart = cartWithLine(activeProduct, 2);
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.of(cart));

        Optional<OrderResponse> response = cartService.removeLine(1L, 100L);

        assertThat(response).isEmpty();
        verify(orderRepository).delete(cart);
    }

    @Test
    void checkout_confirmsDraftOrder() {
        Order cart = cartWithLine(activeProduct, 2);
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.of(cart));

        OrderResponse response = cartService.checkout(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void checkout_throwsResourceNotFoundException_whenCartIsEmpty() {
        when(orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.DRAFT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.checkout(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCustomerOrder_throwsResourceNotFoundException_whenOrderBelongsToAnotherCustomer() {
        when(orderRepository.findByIdAndCustomerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCustomerOrder(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Order cartWithLine(Product product, int quantity) {
        Order cart = new Order(customer);
        ReflectionTestUtils.setField(cart, "id", 50L);
        OrderLine line = new OrderLine(product, quantity, product.getPrice());
        ReflectionTestUtils.setField(line, "id", 100L);
        cart.addLine(line);
        return cart;
    }
}
