package com.example.iaspringlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ClientControllerTest {

    private ClientService clientService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clientService = mock(ClientService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientController(clientService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithTheClient() throws Exception {
        when(clientService.create(any())).thenReturn(new ClientResponse(1L, "Ada", "Lovelace", "ada@example.com"));

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"ada@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void createRejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingClientReturns404() throws Exception {
        when(clientService.get(42L)).thenThrow(new ResourceNotFoundException("Client introuvable : 42"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/clients/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client introuvable : 42"));
    }
}
