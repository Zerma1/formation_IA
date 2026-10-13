package com.example.iaspringlab;

import com.example.iaspringlab.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@ActiveProfiles("test")
class OrderWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullOrderLifecycle_createConfirmCancel_followsBusinessRules() throws Exception {
        long customerId = createCustomer();
        long activeProductId = createProduct("REF-100", "Keyboard", "49.90", true);
        long inactiveProductId = createProduct("REF-200", "Discontinued Mouse", "19.90", false);

        String orderPayload = """
                {"customerId": %d, "lines": [{"productId": %d, "quantity": 2}]}
                """.formatted(customerId, activeProductId);

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode order = objectMapper.readTree(orderJson);
        assertThat(order.get("status").asText()).isEqualTo("DRAFT");
        assertThat(order.get("total").asDouble()).isEqualTo(99.80);
        long orderId = order.get("id").asLong();

        String inactiveLinePayload = """
                {"customerId": %d, "lines": [{"productId": %d, "quantity": 1}]}
                """.formatted(customerId, inactiveProductId);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveLinePayload))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DRAFT\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private long createCustomer() throws Exception {
        String payload = """
                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada-%d@example.com"}
                """.formatted(System.nanoTime());
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private long createProduct(String reference, String name, String price, boolean active) throws Exception {
        String payload = """
                {"reference": "%s", "name": "%s", "price": %s, "active": %s}
                """.formatted(reference, name, price, active);
        String json = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
