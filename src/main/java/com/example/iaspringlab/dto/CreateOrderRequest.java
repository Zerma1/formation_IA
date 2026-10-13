package com.example.iaspringlab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "L'identifiant du client est obligatoire") Long clientId,
        @NotEmpty(message = "Une commande doit contenir au moins une ligne")
        List<@NotNull(message = "Une ligne de commande est obligatoire") @Valid OrderLineRequest> lines) {
}
