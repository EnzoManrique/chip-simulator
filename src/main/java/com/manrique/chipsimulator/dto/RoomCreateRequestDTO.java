package com.manrique.chipsimulator.dto;

import com.manrique.chipsimulator.model.enums.GameMode;

public record RoomCreateRequestDTO(
        Integer initialChips,
        GameMode gameMode,
        Integer maxRebuys,
        Boolean blindsIncrease,
        Integer handsToIncrease
) {
}
