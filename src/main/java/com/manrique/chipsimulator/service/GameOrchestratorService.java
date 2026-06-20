package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.EndHandRequestDTO;
import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.model.enums.ActionType;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.repository.PotRepository;
import com.manrique.chipsimulator.repository.RoomPlayerRepository;
import com.manrique.chipsimulator.repository.RoomRepository;
import com.manrique.chipsimulator.service.observer.RoomUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(GameOrchestratorService.class);

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final PotRepository potRepository;

    private final GameLifecycleService gameLifecycleService;
    private final BettingService bettingService;
    private final ShowdownService showdownService;
    private final PotService potService;
    private final ApplicationEventPublisher eventPublisher;

    public GameOrchestratorService(RoomRepository roomRepository, RoomPlayerRepository roomPlayerRepository, PotRepository potRepository,
                                   GameLifecycleService gameLifecycleService, BettingService bettingService, ShowdownService showdownService,
                                   PotService potService, ApplicationEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.potRepository = potRepository;
        this.gameLifecycleService = gameLifecycleService;
        this.bettingService = bettingService;
        this.showdownService = showdownService;
        this.potService = potService;
        this.eventPublisher = eventPublisher;
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

        Pot mainPot = Pot.builder().room(room).amount(0).build();
        mainPot = potRepository.save(mainPot);
        gameLifecycleService.initializeHand(room, activePlayers, mainPot);

        roomPlayerRepository.saveAll(orderedPlayers);
        Room savedRoom = roomRepository.save(room);

        eventPublisher.publishEvent(new RoomUpdateEvent(savedRoom, "Juego iniciado"));
        
        checkAndProcessAutoFold(savedRoom);

        String phaseName = savedRoom.getPhase() != null ? savedRoom.getPhase().name() : null;
        return new RoomResponseDTO(
                savedRoom.getCode(),
                savedRoom.getInitialChips(),
                savedRoom.getStatus().name(),
                phaseName,
                savedRoom.getGameMode() != null ? savedRoom.getGameMode().name() : null,
                savedRoom.getMaxRebuys(),
                savedRoom.getBlindsIncrease(),
                savedRoom.getHandsToIncrease()
        );
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
            potService.collectBetsAndBuildPots(room, room.getPlayers());
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
            eventPublisher.publishEvent(new RoomUpdateEvent(room, "Un jugador restante - winner: " + winner.getUser().getUsername()));
            return;
        }

        // Verificar si todos los jugadores restantes están all-in -> Ir a SHOWDOWN
        List<RoomPlayer> playersCanBet = room.getPlayers().stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()) && !Boolean.TRUE.equals(p.getIsAllIn()))
                .toList();

        if (playersCanBet.isEmpty()) {
            // Todos están all-in o fold --ir a SHOWDOWN
            potService.collectBetsAndBuildPots(room, room.getPlayers());
            room.setPhase(BettingPhase.SHOWDOWN);
            roomRepository.save(room);
            eventPublisher.publishEvent(new RoomUpdateEvent(room, "Todos all-in - SHOWDOWN"));
        }

        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        boolean roundAdvanced = gameLifecycleService.checkRoundCompletion(room, orderedPlayers);

        if (roundAdvanced) {
            potService.collectBetsAndBuildPots(room, orderedPlayers);
            bettingService.resetTemporaryBets(orderedPlayers);
        } else {
            bettingService.moveToNextTurn(room, orderedPlayers);
        }

        roomRepository.save(room);
        potRepository.save(activePot);
        roomPlayerRepository.saveAll(orderedPlayers);

        eventPublisher.publishEvent(new RoomUpdateEvent(room, "Jugador actúa: " + player.getUser().getUsername()));

        checkAndProcessAutoFold(room);
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

        eventPublisher.publishEvent(new RoomUpdateEvent(room, "Mano terminada - winners: " + request.winnerUsernames()));
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

        Pot mainPot = Pot.builder().room(room).amount(0).build();
        mainPot = potRepository.save(mainPot);
        gameLifecycleService.initializeHand(room, activePlayers, mainPot);

        roomRepository.save(room);
        roomPlayerRepository.saveAll(orderedPlayers);

        eventPublisher.publishEvent(new RoomUpdateEvent(room, "Nueva mano iniciada"));

        checkAndProcessAutoFold(room);
    }

    @Transactional
    public void setUserConnectionStatus(String roomCode, String username, boolean isConnected) {
        Room room = roomRepository.findByCode(roomCode).orElse(null);
        if (room == null) return;

        RoomPlayer player = room.getPlayers().stream()
                .filter(p -> p.getUser().getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);

        if (player != null) {
            player.setIsConnected(isConnected);
            roomPlayerRepository.save(player);
            logger.info("Estado de conexión del jugador {} en la sala {} actualizado a: {}", username, roomCode, isConnected);

            // Notificar la actualización de conexión
            eventPublisher.publishEvent(new RoomUpdateEvent(room, "Conexión: " + username + " está " + (isConnected ? "conectado" : "desconectado")));

            // Si se desconectó y es su turno actual, procesar auto-fold
            if (!isConnected && room.getStatus() == RoomStatus.PLAYING && room.getTurnSeat().equals(player.getSeatNumber())) {
                logger.info("El jugador del turno actual ({}) se desconectó. Ejecutando Auto-Fold.", username);
                try {
                    handlePlayerAction(roomCode, username, new PlayerActionRequestDTO(ActionType.FOLD, null));
                } catch (Exception e) {
                    logger.error("Error al procesar Auto-Fold automático para el jugador " + username, e);
                }
            }
        }
    }

    private void checkAndProcessAutoFold(Room room) {
        if (room.getStatus() != RoomStatus.PLAYING) {
            return;
        }

        RoomPlayer currentPlayer = room.getPlayers().stream()
                .filter(p -> p.getSeatNumber().equals(room.getTurnSeat()))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null && Boolean.TRUE.equals(currentPlayer.getInHand()) && !Boolean.TRUE.equals(currentPlayer.getIsConnected())) {
            logger.info("El jugador del turno actual ({}) está desconectado. Ejecutando Auto-Fold automático.", currentPlayer.getUser().getUsername());
            try {
                handlePlayerAction(room.getCode(), currentPlayer.getUser().getUsername(), new PlayerActionRequestDTO(ActionType.FOLD, null));
            } catch (Exception e) {
                logger.error("Error al procesar Auto-Fold recursivo para el jugador " + currentPlayer.getUser().getUsername(), e);
            }
        }
    }
}
