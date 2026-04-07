package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.RoomCreateRequestDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.enums.RoomPhase;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public RoomResponseDTO createRoom(RoomCreateRequestDTO request) {
        String code;
        do {
            code = generateCode(4);
        } while (roomRepository.findByCode(code).isPresent()); // Garantiza que nunca se repita un codigo de sala
        Room room = Room.builder()
                .code(code)
                .status(RoomStatus.WAITING)
                .phase(RoomPhase.PRE_FLOP)
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
}
