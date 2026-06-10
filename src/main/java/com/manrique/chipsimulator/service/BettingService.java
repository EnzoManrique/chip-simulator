package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BettingService {

    public void processAction(Room room, RoomPlayer player, Pot activePot, PlayerActionRequestDTO request) {
        if (room.getPhase() == BettingPhase.SHOWDOWN || room.getPhase() == null) {
            throw new RuntimeException("La ronda de apuestas ha terminado");
        }

        if (!Boolean.TRUE.equals(player.getInHand())) {
            throw new RuntimeException("El jugador no está en la mano actual");
        }

        if (!room.getTurnSeat().equals(player.getSeatNumber())) {
            throw new RuntimeException("No es tu turno");
        }

        // Si el jugador ya está all-in, no puede actuar más - solo pasa su turno
        if (Boolean.TRUE.equals(player.getIsAllIn())) {
            // El jugador all-in no puede hacer nada, solo esperar
            throw new RuntimeException("El jugador ya está all-in y no puede actuar más");
        }

        switch (request.action()) {
            case FOLD:
                player.setInHand(false);
                if (room.getPots() != null) {
                    for (Pot pot : room.getPots()) {
                        if (pot.getEligiblePlayers() != null) {
                            pot.getEligiblePlayers().remove(player);
                        }
                    }
                }
                break;
            case CHECK:
                break;
            case CALL:
                int maxAvailable = player.getChipsBalance();
                int callAmountNeeded = room.getHighestBet() - (player.getCurrentBet() == null ? 0 : player.getCurrentBet());
                int callAmountToPay = Math.min(callAmountNeeded, maxAvailable);
                
                player.setChipsBalance(player.getChipsBalance() - callAmountToPay);
                player.setCurrentBet((player.getCurrentBet() == null ? 0 : player.getCurrentBet()) + callAmountToPay);
                
                // Si se quedó sin fichas, es all-in
                if (player.getChipsBalance() <= 0) {
                    player.setIsAllIn(true);
                    player.setChipsBalance(0);
                }
                break;
            case RAISE:
                if (request.amount() == null || request.amount() <= room.getHighestBet()) {
                    throw new RuntimeException("El monto a subir debe ser mayor a la apuesta más alta");
                }
                int raiseAmountToPay = request.amount() - (player.getCurrentBet() == null ? 0 : player.getCurrentBet());
                if (raiseAmountToPay > player.getChipsBalance()) {
                    throw new RuntimeException("No tienes suficientes fichas para subir a esa cantidad");
                }
                
                player.setChipsBalance(player.getChipsBalance() - raiseAmountToPay);
                room.setHighestBet(request.amount());
                player.setCurrentBet(request.amount());
                
                // Si se quedó sin fichas, es all-in
                if (player.getChipsBalance() <= 0) {
                    player.setIsAllIn(true);
                    player.setChipsBalance(0);
                }
                break;
            default:
                throw new RuntimeException("Acción no válida");
        }

        player.setHasActed(true);
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

        // Saltar al siguiente jugador que esté en la mano Y no esté all-in
        for (int i = 1; i <= orderedPlayers.size(); i++) {
            int indexToCheck = (currentIndex + i) % orderedPlayers.size();
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

    public void resetTemporaryBets(List<RoomPlayer> orderedPlayers) {
        for (RoomPlayer p : orderedPlayers) {
            p.setCurrentBet(0);
            p.setHasActed(false);
        }
    }
}
