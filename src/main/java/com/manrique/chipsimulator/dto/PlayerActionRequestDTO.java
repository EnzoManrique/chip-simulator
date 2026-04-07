package com.manrique.chipsimulator.dto;

import com.manrique.chipsimulator.model.enums.ActionType;
import jakarta.validation.constraints.NotNull;

public record PlayerActionRequestDTO(
        @NotNull(message = "La acción no puede ser nula") ActionType action,
        Integer amount
) {
}
