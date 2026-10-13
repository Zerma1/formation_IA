package com.example.iaspringlab.product;

import com.example.iaspringlab.account.SecurityConfig;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void create_returns201_whenRequestIsValid() throws Exception {
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true);
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(new ProductResponse(1L, "REF-1", "Widget", new BigDecimal("10.00"), true));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("REF-1"));
    }

    @Test
    void create_returns400_whenPriceIsZero() throws Exception {
        ProductRequest request = new ProductRequest("REF-1", "Widget", BigDecimal.ZERO, true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenReferenceIsBlank() throws Exception {
        ProductRequest request = new ProductRequest("", "Widget", new BigDecimal("10.00"), true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenPriceIsNegative() throws Exception {
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("-5.00"), true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200WithProduct_whenFound() throws Exception {
        when(productService.getById(1L))
                .thenReturn(new ProductResponse(1L, "REF-1", "Widget", new BigDecimal("10.00"), true));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("REF-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getById_returns404_whenProductMissing() throws Exception {
        when(productService.getById(eq(99L))).thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returns200WithAllProducts() throws Exception {
        when(productService.getAll()).thenReturn(List.of(
                new ProductResponse(1L, "REF-1", "Widget", new BigDecimal("10.00"), true),
                new ProductResponse(2L, "REF-2", "Gadget", new BigDecimal("5.00"), false)
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void update_returns200WithUpdatedProduct_whenRequestIsValid() throws Exception {
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("12.00"), false);
        when(productService.update(eq(1L), any(ProductRequest.class)))
                .thenReturn(new ProductResponse(1L, "REF-1", "Widget", new BigDecimal("12.00"), false));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(12.00))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void update_returns404_whenProductMissing() throws Exception {
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("12.00"), false);
        when(productService.update(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204_whenProductExists() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
