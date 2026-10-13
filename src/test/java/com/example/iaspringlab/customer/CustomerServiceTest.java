package com.example.iaspringlab.customer;

import com.example.iaspringlab.exception.DuplicateEmailException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        existingCustomer = new Customer("Ada", "Lovelace", "ada@example.com");
        ReflectionTestUtils.setField(existingCustomer, "id", 1L);
    }

    @Test
    void create_savesCustomer_whenEmailIsNotTaken() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);

        CustomerResponse response = customerService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void create_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void getById_returnsCustomer_whenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));

        CustomerResponse response = customerService.getById(1L);

        assertThat(response.firstName()).isEqualTo("Ada");
    }

    @Test
    void getById_throwsResourceNotFoundException_whenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesFields_whenEmailUnchanged() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        CustomerRequest request = new CustomerRequest("Augusta", "Lovelace", "ada@example.com");

        CustomerResponse response = customerService.update(1L, request);

        assertThat(response.firstName()).isEqualTo("Augusta");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void update_changesEmail_whenNewEmailIsUnique() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailIgnoreCase("ada.lovelace@example.com")).thenReturn(false);
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada.lovelace@example.com");

        CustomerResponse response = customerService.update(1L, request);

        assertThat(response.email()).isEqualTo("ada.lovelace@example.com");
    }

    @Test
    void update_throwsDuplicateEmailException_whenNewEmailAlreadyUsedByAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailIgnoreCase("other@example.com")).thenReturn(true);
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "other@example.com");

        assertThatThrownBy(() -> customerService.update(1L, request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void delete_removesCustomer_whenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));

        customerService.delete(1L);

        verify(customerRepository).delete(existingCustomer);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsAllCustomers() {
        Customer other = new Customer("Grace", "Hopper", "grace@example.com");
        ReflectionTestUtils.setField(other, "id", 2L);
        when(customerRepository.findAll()).thenReturn(java.util.List.of(existingCustomer, other));

        java.util.List<CustomerResponse> responses = customerService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CustomerResponse::email)
                .containsExactly("ada@example.com", "grace@example.com");
    }
}
