package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByReference(String reference);

    boolean existsByReference(String reference);
}
