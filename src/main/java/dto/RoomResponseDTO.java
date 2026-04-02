package dto;

public record RoomResponseDTO(
        String code,
        Integer initialChips,
        String status,
        String phase
) {}
