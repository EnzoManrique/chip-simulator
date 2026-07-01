package com.manrique.chipsimulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Recovery PIN is required")
    @Size(min = 4, max = 6, message = "Recovery PIN must be between 4 and 6 characters")
    private String recoveryPin;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
