package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
        @NotNull(message = "Le statut actif est obligatoire") Boolean active) {
}
