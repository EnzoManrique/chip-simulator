package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import java.util.List;

public abstract class AbstractBettingPhaseState implements BettingPhaseState {

    protected void advanceStandardPhase(Room room, List<RoomPlayer> orderedPlayers, BettingPhase nextPhase) {
        List<RoomPlayer> playersCanBet = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()) && !Boolean.TRUE.equals(p.getIsAllIn()))
                .toList();
        List<RoomPlayer> playersInHand = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (playersInHand.size() > 1 && playersCanBet.size() <= 1) {
            room.setPhase(BettingPhase.SHOWDOWN);
        } else {
            room.setPhase(nextPhase);
        }
        room.setHighestBet(0);

        // Determinar siguiente TurnSeat (el primer jugador activo después del dealer)
        int nextDealerIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (orderedPlayers.get(i).getSeatNumber().equals(room.getDealerSeat())) {
                nextDealerIndex = i;
                break;
            }
        }
        if (nextDealerIndex == -1) nextDealerIndex = 0;

        int nextTurnSeat = -1;
        for (int i = 1; i <= orderedPlayers.size(); i++) {
            int indexToCheck = (nextDealerIndex + i) % orderedPlayers.size();
            RoomPlayer p = orderedPlayers.get(indexToCheck);
            if (Boolean.TRUE.equals(p.getInHand()) && !Boolean.TRUE.equals(p.getIsAllIn())) {
                nextTurnSeat = p.getSeatNumber();
                break;
            }
        }

        if (nextTurnSeat != -1) {
            room.setTurnSeat(nextTurnSeat);
        }
    }
}
