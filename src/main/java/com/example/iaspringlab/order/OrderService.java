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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository,
                         ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public OrderResponse create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + request.customerId()));

        List<Long> productIds = request.lines().stream().map(OrderLineRequest::productId).distinct().toList();
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = new Order(customer);
        for (OrderLineRequest lineRequest : request.lines()) {
            Product product = productsById.get(lineRequest.productId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found with id " + lineRequest.productId());
            }
            if (!product.isActive()) {
                throw new InactiveProductException("Product '" + product.getReference() + "' is inactive and cannot be added to an order.");
            }
            order.addLine(new OrderLine(product, lineRequest.quantity(), product.getPrice()));
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return OrderResponse.from(findOrderOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAll(Long customerId) {
        List<Order> orders = customerId != null
                ? orderRepository.findByCustomerId(customerId)
                : orderRepository.findAll();
        return orders.stream().map(OrderResponse::from).toList();
    }

    public OrderResponse changeStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findOrderOrThrow(id);
        OrderStatus target = request.status();
        if (!order.getStatus().canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition order from " + order.getStatus() + " to " + target + ".");
        }
        order.setStatus(target);
        return OrderResponse.from(order);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }
}
