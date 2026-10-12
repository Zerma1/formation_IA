package com.example.iaspringlab.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String reference,
        String name,
        BigDecimal price,
        boolean active) {
}
