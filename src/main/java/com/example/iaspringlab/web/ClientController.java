package com.example.iaspringlab.web;

import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @GetMapping
    public List<ClientResponse> findAll() {
        return clientService.findAll();
    }
}
