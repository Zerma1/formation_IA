package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
