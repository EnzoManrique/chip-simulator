package com.manrique.chipsimulator.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record EndHandRequestDTO(
        @NotEmpty(message = "Debe haber al menos un ganador") List<String> winnerUsernames
) {
}
