package com.manrique.chipsimulator.dto;

import java.util.List;

public record RoomUpdateDTO(
        String code,
        Integer initialChips,
        String status,
        String phase,
        List<PlayerDTO> players,
        PotDTO mainPot,
        String currentPlayerUsername,
        String lastAction
) {
    public record PlayerDTO(
            String username,
            Integer seatNumber,
            Integer chips,
            Integer currentBet,
            Boolean inHand,
            Boolean isAllIn
    ) {}

    public record PotDTO(
            Integer amount,
            List<String> eligiblePlayerUsernames
    ) {}
}