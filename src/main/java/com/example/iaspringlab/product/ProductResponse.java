package com.example.iaspringlab.product;

import com.example.iaspringlab.category.CategoryResponse;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record ProductResponse(
        Long id,
        String reference,
        String name,
        BigDecimal price,
        boolean active,
        List<CategoryResponse> categories
) {

    public ProductResponse(Long id, String reference, String name, BigDecimal price, boolean active) {
        this(id, reference, name, price, active, List.of());
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getPrice(),
                product.isActive(),
                product.getCategories().stream()
                        .map(CategoryResponse::from)
                        .sorted(Comparator.comparing(CategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
                        .toList()
        );
    }
}
