package com.example.iaspringlab.order;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(

        @NotNull(message = "status is required")
        OrderStatus status
) {
}
