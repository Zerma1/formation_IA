package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Un client existe déjà avec l'email : " + request.email());
        }
        Client client = new Client(request.firstName(), request.lastName(), request.email());
        return DtoMapper.toClientResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        return DtoMapper.toClientResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(DtoMapper::toClientResponse)
                .toList();
    }

    private Client load(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }
}
