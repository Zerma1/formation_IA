package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReference(String reference);
}
