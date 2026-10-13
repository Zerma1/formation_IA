package com.example.iaspringlab.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "La référence est obligatoire") String reference,
        @NotBlank(message = "Le nom est obligatoire") String name,
        @NotNull(message = "Le prix est obligatoire")
        @Positive(message = "Le prix doit être strictement positif")
        @Digits(integer = 17, fraction = 2, message = "Le prix doit avoir au plus 2 décimales")
        BigDecimal price) {
}
