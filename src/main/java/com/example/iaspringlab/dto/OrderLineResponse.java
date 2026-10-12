package com.example.iaspringlab.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long id,
        Long productId,
        String productReference,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
