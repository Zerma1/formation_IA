package com.example.iaspringlab.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String reference,
        String name,
        BigDecimal price,
        boolean active
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getPrice(),
                product.isActive()
        );
    }
}
