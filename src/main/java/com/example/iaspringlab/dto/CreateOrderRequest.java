package com.example.iaspringlab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long clientId,
        @NotEmpty @Valid List<OrderLineRequest> lines) {
}
