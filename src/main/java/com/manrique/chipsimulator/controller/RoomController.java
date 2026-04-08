package com.manrique.chipsimulator.controller;

import com.manrique.chipsimulator.dto.RoomCreateRequestDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.manrique.chipsimulator.service.RoomService;
import com.manrique.chipsimulator.dto.JoinRoomRequestDTO;
import com.manrique.chipsimulator.dto.RoomPlayerResponseDTO;
import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.dto.EndHandRequestDTO;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody RoomCreateRequestDTO request) {
        RoomResponseDTO response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponseDTO> getRoomByCode(@PathVariable String code) {
        RoomResponseDTO response = roomService.getRoomByCode(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomPlayerResponseDTO> joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequestDTO request) {
        RoomPlayerResponseDTO response = roomService.joinRoom(code, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<RoomResponseDTO> startGame(@PathVariable String code) {
        RoomResponseDTO response = roomService.startGame(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/action/{username}")
    public ResponseEntity<Void> processAction(@PathVariable String code, @PathVariable String username, @Valid @RequestBody PlayerActionRequestDTO request) {
        roomService.processAction(code, username, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/end-hand")
    public ResponseEntity<Void> endHand(@PathVariable String code, @Valid @RequestBody EndHandRequestDTO request) {
        roomService.endHand(code, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/next-hand")
    public ResponseEntity<Void> startNextHand(@PathVariable String code) {
        roomService.startNextHand(code);
        return ResponseEntity.ok().build();
    }
}