package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BettingService {

    public void processAction(Room room, RoomPlayer player, Pot activePot, PlayerActionRequestDTO request) {
        if (!Boolean.TRUE.equals(player.getInHand())) {
            throw new RuntimeException("El jugador no está en la mano actual");
        }

        if (!room.getTurnSeat().equals(player.getSeatNumber())) {
            throw new RuntimeException("No es tu turno");
        }

        switch (request.action()) {
            case FOLD:
                player.setInHand(false);
                break;
            case CHECK:
                break;
            case CALL:
                int callAmountToPay = room.getHighestBet() - (player.getCurrentBet() == null ? 0 : player.getCurrentBet());
                player.setChipsBalance(player.getChipsBalance() - callAmountToPay);
                activePot.setAmount(activePot.getAmount() + callAmountToPay);
                player.setCurrentBet(room.getHighestBet());
                if (!activePot.getEligiblePlayers().contains(player)) {
                    activePot.getEligiblePlayers().add(player);
                }
                break;
            case RAISE:
                if (request.amount() == null || request.amount() <= room.getHighestBet()) {
                    throw new RuntimeException("El monto a subir debe ser mayor a la apuesta más alta");
                }
                int raiseAmountToPay = request.amount() - (player.getCurrentBet() == null ? 0 : player.getCurrentBet());
                player.setChipsBalance(player.getChipsBalance() - raiseAmountToPay);
                activePot.setAmount(activePot.getAmount() + raiseAmountToPay);
                room.setHighestBet(request.amount());
                player.setCurrentBet(request.amount());
                if (!activePot.getEligiblePlayers().contains(player)) {
                    activePot.getEligiblePlayers().add(player);
                }
                break;
            default:
                throw new RuntimeException("Acción no válida");
        }
    }

    public void moveToNextTurn(Room room, List<RoomPlayer> orderedPlayers) {
        if (orderedPlayers.isEmpty()) return;

        int currentTurnSeat = room.getTurnSeat();
        int nextTurnSeat = -1;

        int currentIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (orderedPlayers.get(i).getSeatNumber().equals(currentTurnSeat)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) currentIndex = 0;

        for (int i = 1; i <= orderedPlayers.size(); i++) {
            int indexToCheck = (currentIndex + i) % orderedPlayers.size();
            RoomPlayer p = orderedPlayers.get(indexToCheck);
            if (Boolean.TRUE.equals(p.getInHand())) {
                nextTurnSeat = p.getSeatNumber();
                break;
            }
        }

        if (nextTurnSeat != -1) {
            room.setTurnSeat(nextTurnSeat);
        }
    }

    public void resetTemporaryBets(List<RoomPlayer> orderedPlayers) {
        for (RoomPlayer p : orderedPlayers) {
            p.setCurrentBet(0);
        }
    }
}
