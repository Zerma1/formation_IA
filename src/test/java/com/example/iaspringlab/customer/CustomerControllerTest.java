package com.example.iaspringlab.customer;

import com.example.iaspringlab.exception.DuplicateEmailException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    void create_returns201_whenRequestIsValid() throws Exception {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(customerService.create(any(CustomerRequest.class)))
                .thenReturn(new CustomerResponse(1L, "Ada", "Lovelace", "ada@example.com"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void create_returns400_whenEmailIsBlank() throws Exception {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenEmailAlreadyExists() throws Exception {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DuplicateEmailException("A customer with email 'ada@example.com' already exists."));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns400_whenEmailIsMalformed() throws Exception {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "not-an-email");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenDatabaseRejectsDuplicateEmailConcurrently() throws Exception {
        // Covers the GlobalExceptionHandler mapping for DataIntegrityViolationException -> 409,
        // which otherwise is only exercised by the Testcontainers-based repository integration test.
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_returns200WithCustomer_whenFound() throws Exception {
        when(customerService.getById(1L))
                .thenReturn(new CustomerResponse(1L, "Ada", "Lovelace", "ada@example.com"));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void getById_returns404_whenCustomerMissing() throws Exception {
        when(customerService.getById(eq(99L))).thenThrow(new ResourceNotFoundException("Customer not found with id 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returns200WithAllCustomers() throws Exception {
        when(customerService.getAll()).thenReturn(List.of(
                new CustomerResponse(1L, "Ada", "Lovelace", "ada@example.com"),
                new CustomerResponse(2L, "Grace", "Hopper", "grace@example.com")
        ));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].email").value("grace@example.com"));
    }

    @Test
    void update_returns200WithUpdatedCustomer_whenRequestIsValid() throws Exception {
        CustomerRequest request = new CustomerRequest("Augusta", "Lovelace", "ada@example.com");
        when(customerService.update(eq(1L), any(CustomerRequest.class)))
                .thenReturn(new CustomerResponse(1L, "Augusta", "Lovelace", "ada@example.com"));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Augusta"));
    }

    @Test
    void update_returns409_whenNewEmailAlreadyExists() throws Exception {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "taken@example.com");
        when(customerService.update(eq(1L), any(CustomerRequest.class)))
                .thenThrow(new DuplicateEmailException("A customer with email 'taken@example.com' already exists."));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns204_whenCustomerExists() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenCustomerMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Customer not found with id 99"))
                .when(customerService).delete(99L);

        mockMvc.perform(delete("/api/customers/99"))
                .andExpect(status().isNotFound());
    }
}
