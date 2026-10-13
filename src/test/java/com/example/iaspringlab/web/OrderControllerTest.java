package com.example.iaspringlab.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.OrderLineResponse;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    private static OrderResponse draftOrder() {
        return new OrderResponse(1L, 1L, "Ada Lovelace", OrderStatus.DRAFT,
                List.of(new OrderLineResponse(100L, 1L, "REF-1", 2, new BigDecimal("10.00"), new BigDecimal("20.00"))),
                new BigDecimal("20.00"));
    }

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithTheOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(draftOrder());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(20.00))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void createRejectsEmptyLinesWith400AndDetail() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requête invalide"))
                .andExpect(jsonPath("$.details[0]", containsString("lines")));
    }

    @Test
    void createRejectsNonPositiveQuantityWith400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("quantity")));
    }

    @Test
    void createRejectsNullLineWith400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new BusinessRuleException("Règle métier violée"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Règle métier violée"));
    }

    @Test
    void addLineReturns200WithRecomputedTotal() throws Exception {
        when(orderService.addLine(eq(1L), any())).thenReturn(draftOrder());

        mockMvc.perform(post("/api/orders/1/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":2,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void confirmReturns200WithConfirmedStatus() throws Exception {
        when(orderService.confirm(1L)).thenReturn(new OrderResponse(1L, 1L, "Ada Lovelace",
                OrderStatus.CONFIRMED, List.of(), new BigDecimal("20.00")));

        mockMvc.perform(post("/api/orders/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(orderService.get(42L)).thenThrow(new ResourceNotFoundException("Commande introuvable : 42"));

        mockMvc.perform(get("/api/orders/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Commande introuvable : 42"));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(put("/api/orders/1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
