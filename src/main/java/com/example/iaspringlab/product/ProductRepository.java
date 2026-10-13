package com.example.iaspringlab.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = "categories")
    List<Product> findAll();

    @EntityGraph(attributePaths = "categories")
    List<Product> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "categories")
    List<Product> findDistinctByActiveTrueAndCategoriesIdOrderByNameAsc(Long categoryId);

    List<Product> findByCategoriesId(Long categoryId);
}
