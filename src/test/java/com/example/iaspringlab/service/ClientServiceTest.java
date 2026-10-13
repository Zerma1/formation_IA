package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void createRejectsDuplicateEmail() {
        when(clientRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createSavesClient() {
        when(clientRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = clientService.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com"));

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void getThrowsWhenClientMissing() {
        when(clientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
