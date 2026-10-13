package com.example.iaspringlab.web;

import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{id}/lines")
    public OrderResponse addLine(@PathVariable Long id,
                                 @Valid @RequestBody AddOrderLineRequest request) {
        return orderService.addLine(id, request);
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }
}
