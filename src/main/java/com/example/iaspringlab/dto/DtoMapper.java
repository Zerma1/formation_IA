package com.example.iaspringlab.dto;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderLine;
import com.example.iaspringlab.domain.Product;

/**
 * Converts domain entities to their API representations. Entities are never exposed directly.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static ClientResponse toClientResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail());
    }

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getPrice(),
                product.isActive());
    }

    public static OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getFirstName() + " " + order.getClient().getLastName(),
                order.getStatus(),
                order.getLines().stream().map(DtoMapper::toOrderLineResponse).toList(),
                order.getTotal());
    }

    private static OrderLineResponse toOrderLineResponse(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getReference(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal());
    }
}
