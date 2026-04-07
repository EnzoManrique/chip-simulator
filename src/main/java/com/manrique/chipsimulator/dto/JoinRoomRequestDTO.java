package com.manrique.chipsimulator.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequestDTO(@NotBlank String username) {
}
