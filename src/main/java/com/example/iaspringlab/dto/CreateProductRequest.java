package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String reference,
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price) {
}
