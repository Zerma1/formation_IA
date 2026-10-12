package com.example.iaspringlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email) {
}
