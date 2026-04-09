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
import com.manrique.chipsimulator.repository.RoomPlayerRepository;
import com.manrique.chipsimulator.repository.RoomRepository;
import com.manrique.chipsimulator.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class RoomManagementService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    public RoomManagementService(RoomRepository roomRepository, UserRepository userRepository, RoomPlayerRepository roomPlayerRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomPlayerRepository = roomPlayerRepository;
    }

    @Transactional
    public RoomResponseDTO createRoom(RoomCreateRequestDTO request) {
        String code;
        do {
            code = generateCode(4);
        } while (roomRepository.findByCode(code).isPresent());

        Room room = Room.builder()
                .code(code)
                .status(RoomStatus.WAITING)
                .phase(BettingPhase.PRE_FLOP)
                .initialChips(request.initialChips())
                .build();

        Room savedRoom = roomRepository.save(room);

        return new RoomResponseDTO(
                savedRoom.getCode(),
                savedRoom.getInitialChips(),
                savedRoom.getStatus().name(),
                savedRoom.getPhase().name());
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
        return new RoomResponseDTO(
                room.getCode(),
                room.getInitialChips(),
                room.getStatus().name(),
                room.getPhase().name());
    }

    @Transactional
    public RoomPlayerResponseDTO joinRoom(String roomCode, JoinRoomRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(request.username())
                            .passwordHash("default_hash")
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
}
