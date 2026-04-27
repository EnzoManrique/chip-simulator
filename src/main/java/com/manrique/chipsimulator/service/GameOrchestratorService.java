package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.EndHandRequestDTO;
import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.model.enums.ActionType;
import com.manrique.chipsimulator.repository.PotRepository;
import com.manrique.chipsimulator.repository.RoomPlayerRepository;
import com.manrique.chipsimulator.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameOrchestratorService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final PotRepository potRepository;

    private final GameLifecycleService gameLifecycleService;
    private final BettingService bettingService;
    private final ShowdownService showdownService;
    private final WebSocketNotificationService notificationService;

    public GameOrchestratorService(RoomRepository roomRepository, RoomPlayerRepository roomPlayerRepository, PotRepository potRepository,
                                   GameLifecycleService gameLifecycleService, BettingService bettingService, ShowdownService showdownService,
                                   WebSocketNotificationService notificationService) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.potRepository = potRepository;
        this.gameLifecycleService = gameLifecycleService;
        this.bettingService = bettingService;
        this.showdownService = showdownService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RoomResponseDTO startGame(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        
        gameLifecycleService.startGame(room, orderedPlayers);

        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        Pot mainPot = new Pot();
        mainPot = potRepository.save(mainPot);
        gameLifecycleService.initializeHand(room, activePlayers, mainPot);

        roomPlayerRepository.saveAll(orderedPlayers);
        Room savedRoom = roomRepository.save(room);

        notificationService.notifyRoomUpdate(savedRoom, "Juego iniciado");
        String phaseName = savedRoom.getPhase() != null ? savedRoom.getPhase().name() : null;
        return new RoomResponseDTO(
                savedRoom.getCode(),
                savedRoom.getInitialChips(),
                savedRoom.getStatus().name(),
                phaseName);
    }

    @Transactional
    public void handlePlayerAction(String roomCode, String username, PlayerActionRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new RuntimeException("La partida no está en curso");
        }

        RoomPlayer player = room.getPlayers().stream()
                .filter(p -> p.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado en la sala"));

        List<Pot> pots = room.getPots();
        Pot activePot;
        if (pots.isEmpty()) {
            activePot = Pot.builder().room(room).amount(0).build();
            activePot = potRepository.save(activePot);
            room.getPots().add(activePot);
        } else {
            activePot = pots.get(pots.size() - 1);
        }

        bettingService.processAction(room, player, activePot, request);

        // Verificar si solo queda 1 jugador (fold automático)
        List<RoomPlayer> playersInHand = room.getPlayers().stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (playersInHand.size() == 1) {
            // Un solo jugador restante - gana automáticamente
            RoomPlayer winner = playersInHand.get(0);
            showdownService.endHandAuto(room, winner);
            room.setStatus(RoomStatus.WAITING);
            room.setPhase(null); // Resetear fase
            room.setHighestBet(0);
            room.setTurnSeat(room.getDealerSeat());
            for (RoomPlayer p : room.getPlayers()) {
                p.setCurrentBet(0);
            }
            roomRepository.save(room);
            roomPlayerRepository.saveAll(room.getPlayers());
            notificationService.notifyRoomUpdate(room, "Un jugador restante - winner: " + winner.getUser().getUsername());
            return;
        }

        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        boolean roundAdvanced = gameLifecycleService.checkRoundCompletion(room, orderedPlayers);

        if (roundAdvanced) {
            bettingService.resetTemporaryBets(orderedPlayers);
        } else {
            bettingService.moveToNextTurn(room, orderedPlayers);
        }

        roomRepository.save(room);
        potRepository.save(activePot);
        roomPlayerRepository.saveAll(orderedPlayers);

        notificationService.notifyRoomUpdate(room, "Jugador actúa: " + player.getUser().getUsername());
    }

    @Transactional
    public void endHand(String roomCode, EndHandRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new RuntimeException("La partida no está en curso");
        }

        showdownService.endHand(room, request);

        // Resetear room para próxima mano
        room.setStatus(RoomStatus.WAITING);
        room.setPhase(null);
        room.setHighestBet(0);
        room.setTurnSeat(room.getDealerSeat());
        for (RoomPlayer p : room.getPlayers()) {
            p.setCurrentBet(0);
        }

        roomRepository.save(room);
        roomPlayerRepository.saveAll(room.getPlayers());

        notificationService.notifyRoomUpdate(room, "Mano terminada - winners: " + request.winnerUsernames());
    }

    @Transactional
    public void startNextHand(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());

        gameLifecycleService.startNextHand(room, orderedPlayers);

        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        Pot mainPot = new Pot();
        mainPot = potRepository.save(mainPot);
        gameLifecycleService.initializeHand(room, activePlayers, mainPot);

        roomRepository.save(room);
        roomPlayerRepository.saveAll(orderedPlayers);

        notificationService.notifyRoomUpdate(room, "Nueva mano iniciada");
    }
}
