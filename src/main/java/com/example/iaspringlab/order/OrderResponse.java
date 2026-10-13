package com.example.iaspringlab.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        Instant createdAt,
        List<OrderLineResponse> lines,
        BigDecimal total
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList(),
                order.total()
        );
    }
}
