package com.example.iaspringlab.dto;

import com.example.iaspringlab.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        Long clientId,
        String clientName,
        OrderStatus status,
        List<OrderLineResponse> lines,
        BigDecimal total) {
}
