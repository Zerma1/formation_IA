package com.example.iaspringlab.order;

import com.example.iaspringlab.account.SecurityConfig;
import com.example.iaspringlab.exception.InactiveProductException;
import com.example.iaspringlab.exception.InvalidOrderStatusTransitionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void create_returns201_whenRequestIsValid() throws Exception {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, 2)));
        OrderLineResponse line = new OrderLineResponse(1L, 10L, "REF-1", "Widget", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        when(orderService.create(any(OrderRequest.class)))
                .thenReturn(new OrderResponse(1L, 1L, OrderStatus.DRAFT, Instant.now(), List.of(line), new BigDecimal("20.00")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void create_returns400_whenLinesIsEmpty() throws Exception {
        OrderRequest request = new OrderRequest(1L, List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenQuantityIsNotPositive() throws Exception {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, 0)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenQuantityIsNegative() throws Exception {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(10L, -1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenCustomerIdIsMissing() throws Exception {
        OrderRequest request = new OrderRequest(null, List.of(new OrderLineRequest(10L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns422_whenProductIsInactive() throws Exception {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderLineRequest(20L, 1)));
        when(orderService.create(any(OrderRequest.class)))
                .thenThrow(new InactiveProductException("Product 'REF-2' is inactive and cannot be added to an order."));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeStatus_returns422_whenTransitionNotAllowed() throws Exception {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        when(orderService.changeStatus(eq(1L), any(OrderStatusUpdateRequest.class)))
                .thenThrow(new InvalidOrderStatusTransitionException("Cannot transition order from CANCELLED to CONFIRMED."));

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getById_returns200WithOrder_whenFound() throws Exception {
        OrderLineResponse line = new OrderLineResponse(1L, 10L, "REF-1", "Widget", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        when(orderService.getById(1L))
                .thenReturn(new OrderResponse(1L, 1L, OrderStatus.DRAFT, Instant.now(), List.of(line), new BigDecimal("20.00")));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void getById_returns404_whenOrderMissing() throws Exception {
        when(orderService.getById(99L)).thenThrow(new com.example.iaspringlab.exception.ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsOnlyOrdersForCustomer_whenCustomerIdProvided() throws Exception {
        OrderLineResponse line = new OrderLineResponse(1L, 10L, "REF-1", "Widget", 1, new BigDecimal("10.00"), new BigDecimal("10.00"));
        when(orderService.getAll(eq(1L)))
                .thenReturn(List.of(new OrderResponse(1L, 1L, OrderStatus.DRAFT, Instant.now(), List.of(line), new BigDecimal("10.00"))));

        mockMvc.perform(get("/api/orders").param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    void getAll_returnsEveryOrder_whenNoCustomerIdProvided() throws Exception {
        OrderLineResponse line = new OrderLineResponse(1L, 10L, "REF-1", "Widget", 1, new BigDecimal("10.00"), new BigDecimal("10.00"));
        when(orderService.getAll(isNull()))
                .thenReturn(List.of(new OrderResponse(1L, 1L, OrderStatus.DRAFT, Instant.now(), List.of(line), new BigDecimal("10.00"))));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
