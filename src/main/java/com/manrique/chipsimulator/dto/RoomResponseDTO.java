package com.manrique.chipsimulator.dto;

public record RoomResponseDTO(
        String code,
        Integer initialChips,
        String status,
        String phase,
        String gameMode,
        Integer maxRebuys,
        Boolean blindsIncrease,
        Integer handsToIncrease
) {}
