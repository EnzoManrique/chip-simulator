package com.manrique.chipsimulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    // Can be email or username depending on what the user wants, the user said "Busca al usuario por email o username"
    @NotBlank(message = "Username or Email is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
