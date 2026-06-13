package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.JoinRoomRequestDTO;
import com.manrique.chipsimulator.dto.RoomCreateRequestDTO;
import com.manrique.chipsimulator.dto.RoomPlayerResponseDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.User;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.model.enums.GameMode;
import com.manrique.chipsimulator.repository.RoomPlayerRepository;
import com.manrique.chipsimulator.repository.RoomRepository;
import com.manrique.chipsimulator.repository.UserRepository;
import com.manrique.chipsimulator.service.factory.GameModeFactory;
import com.manrique.chipsimulator.service.factory.GameModeFactoryResolver;
import com.manrique.chipsimulator.service.factory.BuyInBehavior;
import com.manrique.chipsimulator.service.observer.RoomUpdateEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class RoomManagementService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final GameModeFactoryResolver factoryResolver;
    private final ApplicationEventPublisher eventPublisher;
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    public RoomManagementService(RoomRepository roomRepository, UserRepository userRepository, 
                                 RoomPlayerRepository roomPlayerRepository, GameModeFactoryResolver factoryResolver,
                                 ApplicationEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.factoryResolver = factoryResolver;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RoomResponseDTO createRoom(RoomCreateRequestDTO request) {
        String code;
        do {
            code = generateCode(4);
        } while (roomRepository.findByCode(code).isPresent());

        GameMode mode = request.gameMode() != null ? request.gameMode() : GameMode.CASH;
        
        Integer maxRebuys = request.maxRebuys();
        Boolean blindsIncrease = request.blindsIncrease();
        Integer handsToIncrease = request.handsToIncrease();
        
        if (mode == GameMode.CASH) {
            if (blindsIncrease == null) blindsIncrease = false;
        } else { // TOURNAMENT
            if (maxRebuys == null) maxRebuys = 1; // 1 recompra por defecto
            if (blindsIncrease == null) blindsIncrease = true;
            if (handsToIncrease == null) handsToIncrease = 3;
        }

        Room room = Room.builder()
                .code(code)
                .status(RoomStatus.WAITING)
                .phase(BettingPhase.PRE_FLOP)
                .initialChips(request.initialChips() != null ? request.initialChips() : 1000)
                .gameMode(mode)
                .maxRebuys(maxRebuys)
                .blindsIncrease(blindsIncrease)
                .handsToIncrease(handsToIncrease)
                .handCount(0)
                .smallBlindAmount(10)
                .build();

        Room savedRoom = roomRepository.save(room);

        String phaseName = savedRoom.getPhase() != null ? savedRoom.getPhase().name() : null;
        return new RoomResponseDTO(
                savedRoom.getCode(),
                savedRoom.getInitialChips(),
                savedRoom.getStatus().name(),
                phaseName,
                savedRoom.getGameMode().name(),
                savedRoom.getMaxRebuys(),
                savedRoom.getBlindsIncrease(),
                savedRoom.getHandsToIncrease()
        );
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
        }
        return builder.toString();
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomByCode(String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        String phaseName = room.getPhase() != null ? room.getPhase().name() : null;
        return new RoomResponseDTO(
                room.getCode(),
                room.getInitialChips(),
                room.getStatus().name(),
                phaseName,
                room.getGameMode().name(),
                room.getMaxRebuys(),
                room.getBlindsIncrease(),
                room.getHandsToIncrease()
        );
    }

    @Transactional
    public RoomPlayerResponseDTO joinRoom(String roomCode, JoinRoomRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(request.username())
                            .password("default_hash")
                            .build();
                    return userRepository.save(newUser);
                });

        int seatNumber = roomPlayerRepository.countByRoomId(room.getId()) + 1;
        boolean inHand = room.getStatus() == RoomStatus.WAITING;

        RoomPlayer roomPlayer = RoomPlayer.builder()
                .room(room)
                .user(user)
                .seatNumber(seatNumber)
                .chipsBalance(room.getInitialChips())
                .inHand(inHand)
                .build();

        roomPlayerRepository.save(roomPlayer);

        return new RoomPlayerResponseDTO(user.getUsername(), roomPlayer.getSeatNumber(), roomPlayer.getChipsBalance());
    }

    @Transactional
    public void processRebuy(String roomCode, String username) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));

        RoomPlayer player = room.getPlayers().stream()
                .filter(p -> p.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado en la sala"));

        GameModeFactory factory = factoryResolver.getFactory(room.getGameMode());
        BuyInBehavior buyInBehavior = factory.getBuyInBehavior();

        if (!buyInBehavior.canRebuy(room, player)) {
            throw new RuntimeException("No cumples con las condiciones para recomprar fichas en esta modalidad");
        }

        int previousBalance = player.getChipsBalance();
        buyInBehavior.executeRebuy(room, player);
        int newBalance = player.getChipsBalance();
        int addedChips = newBalance - previousBalance;

        roomPlayerRepository.save(player);
        Room savedRoom = roomRepository.save(room);

        eventPublisher.publishEvent(new RoomUpdateEvent(savedRoom, 
                "Recompra: " + username + " compró " + addedChips + " fichas (Total balance: " + newBalance + ")"));
    }
}
