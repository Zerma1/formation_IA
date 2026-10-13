package com.example.iaspringlab.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotEmpty(message = "an order must contain at least one line")
        @Valid
        List<OrderLineRequest> lines
) {
}
