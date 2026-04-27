package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameLifecycleService {

    public void startGame(Room room, List<RoomPlayer> orderedPlayers) {
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("La partida ya empezó");
        }

        if (orderedPlayers.size() < 2) {
            throw new RuntimeException("Se necesitan al menos 2 jugadores para iniciar");
        }

        room.setStatus(RoomStatus.PLAYING);
        room.setPhase(BettingPhase.PRE_FLOP);
        room.setDealerSeat(1);
    }

    public void initializeHand(Room room, List<RoomPlayer> activePlayers, Pot mainPot) {
        if (activePlayers.size() < 2) return;

        int dealerSeat = room.getDealerSeat();
        int dealerIndex = -1;
        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getSeatNumber().equals(dealerSeat)) {
                dealerIndex = i;
                break;
            }
        }
        if (dealerIndex == -1) dealerIndex = 0;

        RoomPlayer sbPlayer;
        RoomPlayer bbPlayer;
        int turnSeat;

        int numPlayers = activePlayers.size();
        if (numPlayers == 2) {
            sbPlayer = activePlayers.get(dealerIndex);
            bbPlayer = activePlayers.get((dealerIndex + 1) % numPlayers);
            turnSeat = sbPlayer.getSeatNumber();
        } else {
            sbPlayer = activePlayers.get((dealerIndex + 1) % numPlayers);
            bbPlayer = activePlayers.get((dealerIndex + 2) % numPlayers);
            turnSeat = activePlayers.get((dealerIndex + 3) % numPlayers).getSeatNumber();
        }

        room.setTurnSeat(turnSeat);

        int sbAmount = room.getSmallBlindAmount();
        int bbAmount = sbAmount * 2;

        sbPlayer.setChipsBalance(sbPlayer.getChipsBalance() - sbAmount);
        sbPlayer.setCurrentBet(sbAmount);
        
        bbPlayer.setChipsBalance(bbPlayer.getChipsBalance() - bbAmount);
        bbPlayer.setCurrentBet(bbAmount);

        mainPot.setRoom(room);
        mainPot.setAmount(sbAmount + bbAmount);
        room.getPots().add(mainPot);
        mainPot.getEligiblePlayers().add(sbPlayer);
        mainPot.getEligiblePlayers().add(bbPlayer);
        room.setHighestBet(bbAmount);
    }

    public void startNextHand(Room room, List<RoomPlayer> orderedPlayers) {
        if (room.getStatus() != RoomStatus.PLAYING && room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("La partida no está en curso");
        }
        
        // Si hay pots sin resolver y no es SHOWDOWN, no puede iniciar nueva mano
        if (room.getPhase() != BettingPhase.SHOWDOWN && room.getPhase() != null && !room.getPots().isEmpty()) {
            throw new RuntimeException("La mano anterior aún no ha terminado");
        }

        for (RoomPlayer player : orderedPlayers) {
            if (player.getChipsBalance() > 0) {
                player.setInHand(true);
            } else {
                player.setInHand(false);
            }
        }

        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (activePlayers.size() >= 2) {
            int currentDealerSeat = room.getDealerSeat();
            int dealerIndex = -1;
            for (int i = 0; i < orderedPlayers.size(); i++) {
                if (orderedPlayers.get(i).getSeatNumber().equals(currentDealerSeat)) {
                    dealerIndex = i;
                    break;
                }
            }
            if (dealerIndex == -1) dealerIndex = 0;

            int nextDealerSeat = -1;
            for (int i = 1; i <= orderedPlayers.size(); i++) {
                int indexToCheck = (dealerIndex + i) % orderedPlayers.size();
                RoomPlayer p = orderedPlayers.get(indexToCheck);
                if (Boolean.TRUE.equals(p.getInHand())) {
                    nextDealerSeat = p.getSeatNumber();
                    break;
                }
            }
            if (nextDealerSeat != -1) {
                room.setDealerSeat(nextDealerSeat);
            }
        }

        room.setPhase(BettingPhase.PRE_FLOP);
        room.setStatus(RoomStatus.PLAYING);
    }

public boolean checkRoundCompletion(Room room, List<RoomPlayer> orderedPlayers) {
        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (activePlayers.isEmpty()) return false;

        // 1. Verificar que las apuestas estén equalizadas
        for (RoomPlayer p : activePlayers) {
            if (p.getCurrentBet() == null || p.getCurrentBet() < room.getHighestBet()) {
                return false; // Aún hay apuestas sin igualar
            }
        }

        // 2. Verificar que todos hayan actuado en esta ronda
        // El turno debe haber pasado por todos los jugadores (turnIndex > dealerIndex)
        int dealerSeat = room.getDealerSeat();
        int currentTurn = room.getTurnSeat();

        int dealerIndex = -1, turnIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (orderedPlayers.get(i).getSeatNumber().equals(dealerSeat)) dealerIndex = i;
            if (orderedPlayers.get(i).getSeatNumber().equals(currentTurn)) turnIndex = i;
        }

        // Si no encontramos, usar defecto
        if (dealerIndex == -1) dealerIndex = 0;
        if (turnIndex == -1) turnIndex = 0;

        // Solo completar si turnIndex > dealerIndex (hemos pasado por todos)
        if (turnIndex <= dealerIndex) {
            return false;
        }

        // 3. Todo OK - avanzar fase
        room.setPhase(getNextPhase(room.getPhase()));
        room.setHighestBet(0);

        // 4. Determinar siguiente TurnSeat (el primer jugador activo después del dealer)
        int nextDealerIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (orderedPlayers.get(i).getSeatNumber().equals(dealerSeat)) {
                nextDealerIndex = i;
                break;
            }
        }
        if (nextDealerIndex == -1) nextDealerIndex = 0;

        int nextTurnSeat = -1;
        for (int i = 1; i <= orderedPlayers.size(); i++) {
            int indexToCheck = (nextDealerIndex + i) % orderedPlayers.size();
            RoomPlayer p = orderedPlayers.get(indexToCheck);
            if (Boolean.TRUE.equals(p.getInHand())) {
                nextTurnSeat = p.getSeatNumber();
                break;
            }
        }

        if (nextTurnSeat != -1) {
            room.setTurnSeat(nextTurnSeat);
        }

        return true;
    }

    public BettingPhase getNextPhase(BettingPhase currentPhase) {
        if (currentPhase == null) return BettingPhase.PRE_FLOP;
        switch (currentPhase) {
            case PRE_FLOP: return BettingPhase.FLOP;
            case FLOP: return BettingPhase.TURN;
            case TURN: return BettingPhase.RIVER;
            case RIVER: return BettingPhase.SHOWDOWN;
            default: return currentPhase;
        }
    }
}
