package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
        @NotNull Boolean active) {
}
