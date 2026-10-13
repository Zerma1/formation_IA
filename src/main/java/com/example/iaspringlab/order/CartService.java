package com.example.iaspringlab.order;

import com.example.iaspringlab.customer.Customer;
import com.example.iaspringlab.customer.CustomerRepository;
import com.example.iaspringlab.exception.InactiveProductException;
import com.example.iaspringlab.exception.InvalidOrderStatusTransitionException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.product.Product;
import com.example.iaspringlab.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Shopping cart of a customer. The cart is the customer's DRAFT order: it is persisted while the
 * customer shops and becomes CONFIRMED on checkout.
 */
@Service
@Transactional
public class CartService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CartService(OrderRepository orderRepository, CustomerRepository customerRepository,
                       ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getCart(Long customerId) {
        return findCart(customerId).map(OrderResponse::from);
    }

    public OrderResponse addProduct(Long customerId, Long productId, Integer quantity) {
        requirePositive(quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        if (!product.isActive()) {
            throw new InactiveProductException("Product '" + product.getReference() + "' is inactive and cannot be added to an order.");
        }

        Order cart = findCart(customerId).orElseGet(() -> new Order(findCustomerOrThrow(customerId)));
        cart.findLineForProduct(productId).ifPresentOrElse(
                line -> line.setQuantity(line.getQuantity() + quantity),
                () -> cart.addLine(new OrderLine(product, quantity, product.getPrice()))
        );
        return OrderResponse.from(orderRepository.save(cart));
    }

    public OrderResponse updateQuantity(Long customerId, Long lineId, Integer quantity) {
        requirePositive(quantity);
        Order cart = findCartOrThrow(customerId);
        findLineOrThrow(cart, lineId).setQuantity(quantity);
        return OrderResponse.from(cart);
    }

    /**
     * Removes a line from the cart. An order must contain at least one line, so the cart is
     * deleted when its last line is removed.
     */
    public Optional<OrderResponse> removeLine(Long customerId, Long lineId) {
        Order cart = findCartOrThrow(customerId);
        cart.removeLine(findLineOrThrow(cart, lineId));
        if (!cart.hasLines()) {
            orderRepository.delete(cart);
            return Optional.empty();
        }
        return Optional.of(OrderResponse.from(cart));
    }

    public OrderResponse checkout(Long customerId) {
        Order cart = findCartOrThrow(customerId);
        if (!cart.getStatus().canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition order from " + cart.getStatus() + " to " + OrderStatus.CONFIRMED + ".");
        }
        cart.setStatus(OrderStatus.CONFIRMED);
        return OrderResponse.from(cart);
    }

    /**
     * Past orders of the customer (everything except the current cart), most recent first.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(Long customerId) {
        return orderRepository.findByCustomerIdAndStatusNotOrderByCreatedAtDesc(customerId, OrderStatus.DRAFT).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrder(Long customerId, Long orderId) {
        return orderRepository.findByIdAndCustomerId(orderId, customerId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    private Optional<Order> findCart(Long customerId) {
        return orderRepository.findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, OrderStatus.DRAFT);
    }

    private Order findCartOrThrow(Long customerId) {
        return findCart(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("The cart is empty."));
    }

    private OrderLine findLineOrThrow(Order cart, Long lineId) {
        return cart.findLine(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart line not found with id " + lineId));
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private static void requirePositive(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }
    }
}
