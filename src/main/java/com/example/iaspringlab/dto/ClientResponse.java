package com.example.iaspringlab.dto;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String email) {
}
