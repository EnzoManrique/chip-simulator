package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.service.state.BettingPhaseState;
import com.manrique.chipsimulator.service.state.BettingPhaseStateFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameLifecycleService {

    private final BettingPhaseStateFactory stateFactory;

    public GameLifecycleService(BettingPhaseStateFactory stateFactory) {
        this.stateFactory = stateFactory;
    }

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

        for (RoomPlayer p : orderedPlayers) {
            p.setHasActed(false);
            p.setIsAllIn(false);
            p.setCurrentBet(0);
        }
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

        int sbActual = Math.min(sbAmount, sbPlayer.getChipsBalance());
        sbPlayer.setChipsBalance(sbPlayer.getChipsBalance() - sbActual);
        sbPlayer.setCurrentBet(sbActual);
        if (sbPlayer.getChipsBalance() <= 0) {
            sbPlayer.setIsAllIn(true);
            sbPlayer.setChipsBalance(0);
        }
        
        int bbActual = Math.min(bbAmount, bbPlayer.getChipsBalance());
        bbPlayer.setChipsBalance(bbPlayer.getChipsBalance() - bbActual);
        bbPlayer.setCurrentBet(bbActual);
        if (bbPlayer.getChipsBalance() <= 0) {
            bbPlayer.setIsAllIn(true);
            bbPlayer.setChipsBalance(0);
        }

        mainPot.setRoom(room);
        mainPot.setAmount(sbActual + bbActual);
        room.getPots().add(mainPot);
        mainPot.getEligiblePlayers().add(sbPlayer);
        mainPot.getEligiblePlayers().add(bbPlayer);
        room.setHighestBet(Math.max(sbActual, bbActual));

        // Si el jugador del turno inicial está All-In, mover al siguiente que no lo esté
        RoomPlayer initialTurnPlayer = activePlayers.stream()
                .filter(p -> p.getSeatNumber().equals(room.getTurnSeat()))
                .findFirst()
                .orElse(null);

        if (initialTurnPlayer != null && Boolean.TRUE.equals(initialTurnPlayer.getIsAllIn())) {
            int currentIndex = activePlayers.indexOf(initialTurnPlayer);
            int nextTurnSeat = -1;
            for (int i = 1; i <= activePlayers.size(); i++) {
                RoomPlayer p = activePlayers.get((currentIndex + i) % activePlayers.size());
                if (!Boolean.TRUE.equals(p.getIsAllIn())) {
                    nextTurnSeat = p.getSeatNumber();
                    break;
                }
            }
            if (nextTurnSeat != -1) {
                room.setTurnSeat(nextTurnSeat);
            }
        }

        // Si ya no quedan suficientes jugadores que puedan apostar (<= 1) y hay más de 1 en juego, ir a Showdown
        List<RoomPlayer> playersCanBet = activePlayers.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAllIn()))
                .toList();

        if (activePlayers.size() > 1 && playersCanBet.size() <= 1) {
            room.setPhase(BettingPhase.SHOWDOWN);
        }
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
            player.setHasActed(false);
            player.setIsAllIn(false);
            player.setCurrentBet(0);
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
                .filter(p -> Boolean.TRUE.equals(p.getInHand()) && !Boolean.TRUE.equals(p.getIsAllIn()))
                .toList();

        if (activePlayers.isEmpty()) return false;

        // 1. Verificar que todos hayan actuado Y sus apuestas estén equalizadas
        for (RoomPlayer p : activePlayers) {
            if (!Boolean.TRUE.equals(p.getHasActed())) {
                return false; // Alguien aún no ha actuado
            }
            if (p.getCurrentBet() == null || p.getCurrentBet() < room.getHighestBet()) {
                return false; // Aún hay apuestas sin igualar
            }
        }

        // 3. Todo OK - avanzar fase usando el patrón State
        BettingPhaseState currentState = stateFactory.getState(room.getPhase());
        currentState.completeRound(room, orderedPlayers);

        return true;
    }
}
