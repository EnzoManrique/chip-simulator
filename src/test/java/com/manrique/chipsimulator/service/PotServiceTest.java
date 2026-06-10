package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.User;
import com.manrique.chipsimulator.repository.PotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PotServiceTest {

    private PotRepository potRepository;
    private PotService potService;

    @BeforeEach
    void setUp() {
        potRepository = Mockito.mock(PotRepository.class);
        potService = new PotService(potRepository);
        
        // Mock save to return the same Pot passed to it
        when(potRepository.save(any(Pot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCollectBetsSimpleNoAllIn() {
        Room room = new Room();
        room.setPots(new ArrayList<>());

        RoomPlayer p1 = createMockPlayer("J1", 1, 1000, 100, true);
        RoomPlayer p2 = createMockPlayer("J2", 2, 1000, 100, true);
        RoomPlayer p3 = createMockPlayer("J3", 3, 1000, 100, true);

        List<RoomPlayer> players = List.of(p1, p2, p3);

        potService.collectBetsAndBuildPots(room, players);

        // Debería crearse 1 pozo de 300 con J1, J2, J3 como elegibles
        assertEquals(1, room.getPots().size());
        Pot mainPot = room.getPots().get(0);
        assertEquals(300, mainPot.getAmount());
        assertEquals(3, mainPot.getEligiblePlayers().size());
        assertTrue(mainPot.getEligiblePlayers().containsAll(List.of(p1, p2, p3)));

        // Las apuestas actuales de los jugadores deben quedar en 0
        assertEquals(0, p1.getCurrentBet());
        assertEquals(0, p2.getCurrentBet());
        assertEquals(0, p3.getCurrentBet());
    }

    @Test
    void testCollectBetsWithAllInAndRefund() {
        Room room = new Room();
        room.setPots(new ArrayList<>());

        // Escenario del usuario:
        // J1: apuesta 600, inHand = true, balance = 400 (tenía 1000 original)
        // J2: apuesta 243, inHand = true (All-In), balance = 0
        // J3: apuesta 500, inHand = true (All-In), balance = 0
        // J4: apuesta 600, inHand = false (Fold), balance = 400
        RoomPlayer p1 = createMockPlayer("J1", 1, 400, 600, true);
        RoomPlayer p2 = createMockPlayer("J2", 2, 0, 243, true);
        p2.setIsAllIn(true);
        RoomPlayer p3 = createMockPlayer("J3", 3, 0, 500, true);
        p3.setIsAllIn(true);
        RoomPlayer p4 = createMockPlayer("J4", 4, 400, 600, false); // fold

        List<RoomPlayer> players = List.of(p1, p2, p3, p4);

        potService.collectBetsAndBuildPots(room, players);

        // Se deben crear 2 pozos:
        // Pozo 1 (Principal): 243 * 4 = 972. Elegibles: J1, J2, J3.
        // Pozo 2 (Secundario): 257 * 3 = 771. Elegibles: J1, J3.
        // Reembolso para J1: 100 * 2 = 200.
        // Explicación:
        // - Chunk 1 (243): 4 contribuyentes >= 243. Monto = 972. Elegibles inHand: J1, J2, J3.
        // - Chunk 2 (257): 3 contribuyentes >= 257 (J1, J3, J4). Monto = 771. Elegibles inHand: J1, J3.
        // - Chunk 3 (100): 2 contribuyentes >= 100 (J1, J4). Monto = 200. Elegibles inHand: J1.
        //   Como solo hay 1 elegible (J1), se le reembolsa 200 directos a su balance.

        assertEquals(2, room.getPots().size());
        
        Pot pot1 = room.getPots().get(0);
        assertEquals(972, pot1.getAmount());
        assertEquals(3, pot1.getEligiblePlayers().size());
        assertTrue(pot1.getEligiblePlayers().containsAll(List.of(p1, p2, p3)));

        Pot pot2 = room.getPots().get(1);
        assertEquals(771, pot2.getAmount());
        assertEquals(2, pot2.getEligiblePlayers().size());
        assertTrue(pot2.getEligiblePlayers().containsAll(List.of(p1, p3)));

        // Balance de J1 original era 400, con reembolso de 200 debe ser 600.
        assertEquals(600, p1.getChipsBalance());
        
        // Todos los currentBet deben terminar en 0
        assertEquals(0, p1.getCurrentBet());
        assertEquals(0, p2.getCurrentBet());
        assertEquals(0, p3.getCurrentBet());
        assertEquals(0, p4.getCurrentBet());
    }

    private RoomPlayer createMockPlayer(String username, int seat, int balance, int currentBet, boolean inHand) {
        User user = new User();
        user.setUsername(username);
        
        RoomPlayer player = new RoomPlayer();
        player.setUser(user);
        player.setSeatNumber(seat);
        player.setChipsBalance(balance);
        player.setCurrentBet(currentBet);
        player.setInHand(inHand);
        player.setIsAllIn(false);
        player.setIsConnected(true);
        return player;
    }
}
