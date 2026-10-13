package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(
        @NotNull(message = "L'identifiant du produit est obligatoire") Long productId,
        @Positive(message = "La quantité doit être strictement positive") int quantity) {
}
