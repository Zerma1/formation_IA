package com.example.iaspringlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ProductControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithActiveProduct() throws Exception {
        when(productService.create(any())).thenReturn(
                new ProductResponse(1L, "REF-1", "Keyboard", new BigDecimal("49.90"), true));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":49.90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("REF-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRejectsNonPositivePriceWith400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requête invalide"));
    }

    @Test
    void duplicateReferenceReturns409() throws Exception {
        when(productService.create(any())).thenThrow(new DuplicateResourceException("Un produit existe déjà"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":9.99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un produit existe déjà"));
    }

    @Test
    void patchStatusDeactivatesProduct() throws Exception {
        when(productService.updateStatus(eq(1L), any())).thenReturn(
                new ProductResponse(1L, "REF-1", "Keyboard", new BigDecimal("9.99"), false));

        mockMvc.perform(patch("/api/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void patchStatusRejectsMissingActiveWith400() throws Exception {
        mockMvc.perform(patch("/api/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
