package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.RoomUpdateDTO;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía una actualización de room a todos los clientes suscritos al topic de la sala.
     */
    public void notifyRoomUpdate(Room room, String lastAction) {
        RoomUpdateDTO update = toRoomUpdateDTO(room, lastAction);
        // Envía a /topic/room/{roomCode}
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), update);
    }

    /**
     * Envía una actualización solo a un jugador específico (ej. para sus cartas privadas).
     */
    public void notifyPlayerUpdate(String roomCode, String username, RoomUpdateDTO update) {
        // Cola privada para el jugador específico
        messagingTemplate.convertAndSend("/queue/room/" + roomCode + "/player/" + username, update);
    }

    private RoomUpdateDTO toRoomUpdateDTO(Room room, String lastAction) {
        List<RoomUpdateDTO.PlayerDTO> players = room.getPlayers().stream()
                .map(p -> new RoomUpdateDTO.PlayerDTO(
                        p.getUser().getUsername(),
                        p.getSeatNumber(),
                        p.getChipsBalance(),
                        p.getCurrentBet(),
                        p.getInHand(),
                        p.getIsAllIn()
                ))
                .toList();

        RoomUpdateDTO.PotDTO potDTO = null;
        if (!room.getPots().isEmpty()) {
            var pot = room.getPots().get(room.getPots().size() - 1);
            List<String> eligible = pot.getEligiblePlayers().stream()
                    .map(p -> p.getUser().getUsername())
                    .toList();
            potDTO = new RoomUpdateDTO.PotDTO(pot.getAmount(), eligible);
        }

        String currentPlayerUsername = room.getCurrentPlayer() != null
                ? room.getCurrentPlayer().getUser().getUsername()
                : null;

        String phaseName = room.getPhase() != null ? room.getPhase().name() : null;

        return new RoomUpdateDTO(
                room.getCode(),
                room.getInitialChips(),
                room.getStatus().name(),
                phaseName,
                players,
                potDTO,
                currentPlayerUsername,
                lastAction
        );
    }
}