package service;

import dto.RoomCreateRequestDTO;
import dto.RoomResponseDTO;
import model.Room;
import model.enums.RoomPhase;
import model.enums.RoomStatus;
import repository.RoomRepository;
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
}
