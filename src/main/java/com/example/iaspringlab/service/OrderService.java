package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import com.example.iaspringlab.repository.OrderRepository;
import com.example.iaspringlab.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ClientRepository clientRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
    }

    /**
     * Crée une commande DRAFT pour un client, avec au moins une ligne.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleException("Une commande doit contenir au moins une ligne");
        }

        Order order = new Order(client);
        for (OrderLineRequest line : request.lines()) {
            addLineInternal(order, line.productId(), line.quantity());
        }
        return DtoMapper.toOrderResponse(orderRepository.save(order));
    }

    /**
     * Ajoute une ligne à une commande DRAFT.
     */
    @Transactional
    public OrderResponse addLine(Long orderId, AddOrderLineRequest request) {
        Order order = loadOrder(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException("Impossible d'ajouter une ligne à une commande au statut " + order.getStatus());
        }
        addLineInternal(order, request.productId(), request.quantity());
        return DtoMapper.toOrderResponse(order);
    }

    /**
     * Transition DRAFT -> CONFIRMED.
     */
    @Transactional
    public OrderResponse confirm(Long orderId) {
        Order order = loadOrder(orderId);
        order.transitionTo(OrderStatus.CONFIRMED);
        return DtoMapper.toOrderResponse(order);
    }

    /**
     * Transition DRAFT -> CANCELLED ou CONFIRMED -> CANCELLED.
     */
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = loadOrder(orderId);
        order.transitionTo(OrderStatus.CANCELLED);
        return DtoMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(DtoMapper::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return DtoMapper.toOrderResponse(loadOrder(id));
    }

    private void addLineInternal(Order order, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + productId));
        if (!product.isActive()) {
            throw new BusinessRuleException(
                    "Le produit '" + product.getReference() + "' est inactif et ne peut pas être ajouté à une commande");
        }
        order.addLine(product, quantity);
    }

    private Order loadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
    }
}
