package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.RoomCreateRequestDTO;
import com.manrique.chipsimulator.dto.RoomResponseDTO;
import com.manrique.chipsimulator.dto.JoinRoomRequestDTO;
import com.manrique.chipsimulator.dto.RoomPlayerResponseDTO;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.User;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.repository.RoomRepository;
import com.manrique.chipsimulator.repository.RoomPlayerRepository;
import com.manrique.chipsimulator.repository.UserRepository;
import com.manrique.chipsimulator.repository.PotRepository;
import com.manrique.chipsimulator.dto.PlayerActionRequestDTO;
import com.manrique.chipsimulator.dto.EndHandRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final PotRepository potRepository;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository, UserRepository userRepository, RoomPlayerRepository roomPlayerRepository, PotRepository potRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.potRepository = potRepository;
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

    @Transactional
    public RoomResponseDTO startGame(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("La partida ya empezó");
        }

        List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        if (players.size() < 2) {
            throw new RuntimeException("Se necesitan al menos 2 jugadores para iniciar");
        }

        room.setStatus(RoomStatus.PLAYING);
        room.setPhase(BettingPhase.PRE_FLOP);
        room.setDealerSeat(1);

        initializeHand(room);

        Room savedRoom = roomRepository.save(room);

        return new RoomResponseDTO(
                savedRoom.getCode(),
                savedRoom.getInitialChips(),
                savedRoom.getStatus().name(),
                savedRoom.getPhase().name());
    }

    @Transactional
    public void processAction(String roomCode, String username, PlayerActionRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new RuntimeException("La partida no está en curso");
        }

        RoomPlayer player = room.getPlayers().stream()
                .filter(p -> p.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado en la sala"));

        if (!Boolean.TRUE.equals(player.getInHand())) {
            throw new RuntimeException("El jugador no está en la mano actual");
        }

        if (!room.getTurnSeat().equals(player.getSeatNumber())) {
            throw new RuntimeException("No es tu turno");
        }

        List<Pot> pots = room.getPots();
        Pot activePot;
        if (pots.isEmpty()) {
            activePot = Pot.builder().room(room).amount(0).build();
            activePot = potRepository.save(activePot);
            room.getPots().add(activePot);
        } else {
            activePot = pots.get(pots.size() - 1);
        }

        switch (request.action()) {
            case FOLD:
                player.setInHand(false);
                break;
            case CHECK:
                // No descuenta saldo
                break;
            case CALL:
                int callAmountToPay = room.getHighestBet() - player.getCurrentBet();
                player.setChipsBalance(player.getChipsBalance() - callAmountToPay);
                activePot.setAmount(activePot.getAmount() + callAmountToPay);
                player.setCurrentBet(room.getHighestBet());
                if (!activePot.getEligiblePlayers().contains(player)) {
                    activePot.getEligiblePlayers().add(player);
                }
                break;
            case RAISE:
                if (request.amount() == null || request.amount() <= room.getHighestBet()) {
                    throw new RuntimeException("El monto a subir debe ser mayor a la apuesta más alta");
                }
                int raiseAmountToPay = request.amount() - player.getCurrentBet();
                player.setChipsBalance(player.getChipsBalance() - raiseAmountToPay);
                activePot.setAmount(activePot.getAmount() + raiseAmountToPay);
                room.setHighestBet(request.amount());
                player.setCurrentBet(request.amount());
                if (!activePot.getEligiblePlayers().contains(player)) {
                    activePot.getEligiblePlayers().add(player);
                }
                break;
            default:
                throw new RuntimeException("Acción no válida");
        }

        boolean roundAdvanced = checkRoundCompletion(room);

        if (!roundAdvanced) {
            moveToNextTurn(room);
        }

        roomRepository.save(room);
        potRepository.save(activePot);
        roomPlayerRepository.save(player);
    }

    private void moveToNextTurn(Room room) {
        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        if (orderedPlayers.isEmpty()) return;

        int currentTurnSeat = room.getTurnSeat();
        int nextTurnSeat = -1;

        int currentIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (orderedPlayers.get(i).getSeatNumber().equals(currentTurnSeat)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) currentIndex = 0;

        for (int i = 1; i <= orderedPlayers.size(); i++) {
            int indexToCheck = (currentIndex + i) % orderedPlayers.size();
            RoomPlayer p = orderedPlayers.get(indexToCheck);
            if (Boolean.TRUE.equals(p.getInHand())) {
                nextTurnSeat = p.getSeatNumber();
                break;
            }
        }

        if (nextTurnSeat != -1) {
            room.setTurnSeat(nextTurnSeat);
        }
    }

    private boolean checkRoundCompletion(Room room) {
        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (activePlayers.isEmpty()) return false;

        boolean roundComplete = true;
        for (RoomPlayer p : activePlayers) {
            if (p.getCurrentBet() == null || p.getCurrentBet() < room.getHighestBet()) {
                roundComplete = false;
                break;
            }
        }

        if (roundComplete) {
            room.setPhase(getNextPhase(room.getPhase()));
            room.setHighestBet(0);
            
            for (RoomPlayer p : orderedPlayers) {
                p.setCurrentBet(0);
                roomPlayerRepository.save(p);
            }
            
            int dealerSeat = room.getDealerSeat();
            int nextTurnSeat = -1;
            
            int dealerIndex = -1;
            for (int i = 0; i < orderedPlayers.size(); i++) {
                if (orderedPlayers.get(i).getSeatNumber().equals(dealerSeat)) {
                    dealerIndex = i;
                    break;
                }
            }
            if (dealerIndex == -1) dealerIndex = 0;

            for (int i = 1; i <= orderedPlayers.size(); i++) {
                int indexToCheck = (dealerIndex + i) % orderedPlayers.size();
                RoomPlayer p = orderedPlayers.get(indexToCheck);
                if (Boolean.TRUE.equals(p.getInHand())) {
                    nextTurnSeat = p.getSeatNumber();
                    break;
                }
            }

            if (nextTurnSeat != -1) {
                room.setTurnSeat(nextTurnSeat);
            }
            
            return true;
        }
        return false;
    }

    private BettingPhase getNextPhase(BettingPhase currentPhase) {
        if (currentPhase == null) return BettingPhase.PRE_FLOP;
        switch (currentPhase) {
            case PRE_FLOP: return BettingPhase.FLOP;
            case FLOP: return BettingPhase.TURN;
            case TURN: return BettingPhase.RIVER;
            case RIVER: return BettingPhase.SHOWDOWN;
            default: return currentPhase;
        }
    }

    @Transactional
    public void endHand(String roomCode, EndHandRequestDTO request) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new RuntimeException("La partida no está en curso");
        }

        room.setPhase(BettingPhase.SHOWDOWN);

        for (Pot pot : room.getPots()) {
            List<RoomPlayer> eligibleWinners = pot.getEligiblePlayers().stream()
                    .filter(p -> request.winnerUsernames().contains(p.getUser().getUsername()))
                    .toList();

            if (!eligibleWinners.isEmpty()) {
                int splitAmount = pot.getAmount() / eligibleWinners.size();
                int extraChips = pot.getAmount() % eligibleWinners.size();

                for (int i = 0; i < eligibleWinners.size(); i++) {
                    RoomPlayer winner = eligibleWinners.get(i);
                    int amountToAdd = splitAmount;
                    if (i == 0) {
                        amountToAdd += extraChips;
                    }
                    winner.setChipsBalance(winner.getChipsBalance() + amountToAdd);
                }
            }
        }

        room.getPots().clear();
        roomRepository.save(room);
    }

    private void initializeHand(Room room) {
        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (activePlayers.size() < 2) return;

        int dealerSeat = room.getDealerSeat();
        int dealerIndex = -1;
        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getSeatNumber().equals(dealerSeat)) {
                dealerIndex = i;
                break;
            }
        }
        if (dealerIndex == -1) dealerIndex = 0;

        RoomPlayer sbPlayer;
        RoomPlayer bbPlayer;
        int turnSeat;

        int numPlayers = activePlayers.size();
        if (numPlayers == 2) {
            sbPlayer = activePlayers.get(dealerIndex);
            bbPlayer = activePlayers.get((dealerIndex + 1) % numPlayers);
            turnSeat = sbPlayer.getSeatNumber();
        } else {
            sbPlayer = activePlayers.get((dealerIndex + 1) % numPlayers);
            bbPlayer = activePlayers.get((dealerIndex + 2) % numPlayers);
            turnSeat = activePlayers.get((dealerIndex + 3) % numPlayers).getSeatNumber();
        }

        room.setTurnSeat(turnSeat);

        int sbAmount = room.getSmallBlindAmount();
        int bbAmount = sbAmount * 2;

        sbPlayer.setChipsBalance(sbPlayer.getChipsBalance() - sbAmount);
        sbPlayer.setCurrentBet(sbAmount);
        
        bbPlayer.setChipsBalance(bbPlayer.getChipsBalance() - bbAmount);
        bbPlayer.setCurrentBet(bbAmount);

        Pot mainPot = potRepository.save(Pot.builder().room(room).amount(sbAmount + bbAmount).build());
        room.getPots().add(mainPot);
        mainPot.getEligiblePlayers().add(sbPlayer);
        mainPot.getEligiblePlayers().add(bbPlayer);
        room.setHighestBet(bbAmount);

        roomPlayerRepository.saveAll(activePlayers);
    }

    @Transactional
    public void startNextHand(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new RuntimeException("La partida no está en curso");
        }
        
        if (room.getPhase() != BettingPhase.SHOWDOWN && !room.getPots().isEmpty()) {
            throw new RuntimeException("La mano anterior aún no ha terminado");
        }

        List<RoomPlayer> orderedPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        for (RoomPlayer player : orderedPlayers) {
            if (player.getChipsBalance() > 0) {
                player.setInHand(true);
            } else {
                player.setInHand(false);
            }
        }

        List<RoomPlayer> activePlayers = orderedPlayers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                .toList();

        if (activePlayers.size() >= 2) {
            int currentDealerSeat = room.getDealerSeat();
            int dealerIndex = -1;
            for (int i = 0; i < orderedPlayers.size(); i++) {
                if (orderedPlayers.get(i).getSeatNumber().equals(currentDealerSeat)) {
                    dealerIndex = i;
                    break;
                }
            }
            if (dealerIndex == -1) dealerIndex = 0;

            int nextDealerSeat = -1;
            for (int i = 1; i <= orderedPlayers.size(); i++) {
                int indexToCheck = (dealerIndex + i) % orderedPlayers.size();
                RoomPlayer p = orderedPlayers.get(indexToCheck);
                if (Boolean.TRUE.equals(p.getInHand())) {
                    nextDealerSeat = p.getSeatNumber();
                    break;
                }
            }
            if (nextDealerSeat != -1) {
                room.setDealerSeat(nextDealerSeat);
            }
        }

        room.setPhase(BettingPhase.PRE_FLOP);

        initializeHand(room);

        roomRepository.save(room);
        roomPlayerRepository.saveAll(orderedPlayers);
    }
}
