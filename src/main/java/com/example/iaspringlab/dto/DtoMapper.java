package com.example.iaspringlab.dto;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderLine;
import com.example.iaspringlab.domain.Product;
import java.util.List;

/**
 * Conversion entités -> DTO de réponse.
 *
 * <p>Le mapping des commandes doit être réalisé à l'intérieur d'une transaction
 * (les associations étant chargées en LAZY).</p>
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
        List<OrderLineResponse> lines = order.getLines().stream()
                .map(DtoMapper::toOrderLineResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getFullName(),
                order.getStatus(),
                order.getTotal(),
                lines);
    }

    public static OrderLineResponse toOrderLineResponse(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getReference(),
                line.getProduct().getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal());
    }
}
