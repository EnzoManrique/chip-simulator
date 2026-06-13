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
                        p.getIsAllIn(),
                        p.getIsConnected(),
                        p.getRebuyCount()
                ))
                .toList();

        RoomUpdateDTO.PotDTO potDTO = null;
        int totalPotAmount = room.getPots().stream().mapToInt(com.manrique.chipsimulator.model.Pot::getAmount).sum()
                + room.getPlayers().stream().mapToInt(p -> p.getCurrentBet() != null ? p.getCurrentBet() : 0).sum();

        List<String> eligible;
        if (!room.getPots().isEmpty()) {
            eligible = room.getPots().stream()
                    .flatMap(pot -> pot.getEligiblePlayers().stream())
                    .map(p -> p.getUser().getUsername())
                    .distinct()
                    .toList();
        } else {
            eligible = room.getPlayers().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                    .map(p -> p.getUser().getUsername())
                    .toList();
        }
        potDTO = new RoomUpdateDTO.PotDTO(totalPotAmount, eligible);

        String currentPlayerUsername = room.getPlayers().stream()
                .filter(p -> p.getSeatNumber().equals(room.getTurnSeat()))
                .map(p -> p.getUser().getUsername())
                .findFirst()
                .orElse(null);

        String phaseName = room.getPhase() != null ? room.getPhase().name() : null;

        return new RoomUpdateDTO(
                room.getCode(),
                room.getInitialChips(),
                room.getStatus().name(),
                phaseName,
                players,
                potDTO,
                currentPlayerUsername,
                lastAction,
                room.getDealerSeat(),
                room.getGameMode() != null ? room.getGameMode().name() : null,
                room.getMaxRebuys(),
                room.getBlindsIncrease(),
                room.getHandsToIncrease()
        );
    }
}