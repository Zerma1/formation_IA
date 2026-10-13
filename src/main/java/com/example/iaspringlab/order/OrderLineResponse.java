package com.example.iaspringlab.order;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long id,
        Long productId,
        String productReference,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getReference(),
                line.getProduct().getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.lineTotal()
        );
    }
}
