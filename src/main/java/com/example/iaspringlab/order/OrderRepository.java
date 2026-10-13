package com.example.iaspringlab.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    Optional<Order> findFirstByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, OrderStatus status);

    List<Order> findByCustomerIdAndStatusNotOrderByCreatedAtDesc(Long customerId, OrderStatus status);

    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
}
