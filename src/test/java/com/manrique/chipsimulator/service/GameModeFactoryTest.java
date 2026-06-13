package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.GameMode;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import com.manrique.chipsimulator.service.factory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameModeFactoryTest {

    private GameModeFactoryResolver factoryResolver;

    @BeforeEach
    void setUp() {
        CashGameModeFactory cashFactory = new CashGameModeFactory();
        TournamentModeFactory tournamentFactory = new TournamentModeFactory();
        factoryResolver = new GameModeFactoryResolver(cashFactory, tournamentFactory);
    }

    @Test
    void testCashGameBlindStructure_Constant() {
        Room room = new Room();
        room.setGameMode(GameMode.CASH);
        room.setSmallBlindAmount(10);
        room.setBlindsIncrease(false);
        room.setHandCount(0);

        BlindStructure structure = factoryResolver.getFactory(GameMode.CASH).getBlindStructure();

        // Jugar 5 manos - la ciega no debería cambiar
        for (int i = 0; i < 5; i++) {
            structure.setupBlindsForNextHand(room);
        }

        assertEquals(10, room.getSmallBlindAmount());
    }

    @Test
    void testCashGameBlindStructure_WithIncrease() {
        Room room = new Room();
        room.setGameMode(GameMode.CASH);
        room.setSmallBlindAmount(10);
        room.setBlindsIncrease(true);
        room.setHandsToIncrease(2);
        room.setHandCount(0);

        BlindStructure structure = factoryResolver.getFactory(GameMode.CASH).getBlindStructure();

        // 1ra mano
        structure.setupBlindsForNextHand(room);
        assertEquals(10, room.getSmallBlindAmount());

        // 2da mano (debe subir, 2 % 2 == 0)
        structure.setupBlindsForNextHand(room);
        assertEquals(20, room.getSmallBlindAmount());

        // 3ra mano
        structure.setupBlindsForNextHand(room);
        assertEquals(20, room.getSmallBlindAmount());

        // 4ta mano (debe subir de nuevo)
        structure.setupBlindsForNextHand(room);
        assertEquals(40, room.getSmallBlindAmount());
    }

    @Test
    void testTournamentBlindStructure_DoublesEveryThreeHands() {
        Room room = new Room();
        room.setGameMode(GameMode.TOURNAMENT);
        room.setSmallBlindAmount(10);
        room.setBlindsIncrease(true);
        room.setHandsToIncrease(3);
        room.setHandCount(0);

        BlindStructure structure = factoryResolver.getFactory(GameMode.TOURNAMENT).getBlindStructure();

        // Mano 1
        structure.setupBlindsForNextHand(room);
        assertEquals(10, room.getSmallBlindAmount());

        // Mano 2
        structure.setupBlindsForNextHand(room);
        assertEquals(10, room.getSmallBlindAmount());

        // Mano 3 (sube a 20)
        structure.setupBlindsForNextHand(room);
        assertEquals(20, room.getSmallBlindAmount());

        // Mano 4, 5
        structure.setupBlindsForNextHand(room);
        structure.setupBlindsForNextHand(room);
        assertEquals(20, room.getSmallBlindAmount());

        // Mano 6 (sube a 40)
        structure.setupBlindsForNextHand(room);
        assertEquals(40, room.getSmallBlindAmount());
    }

    @Test
    void testCashBuyInBehavior_Limits() {
        Room room = new Room();
        room.setGameMode(GameMode.CASH);
        room.setInitialChips(1000);
        room.setMaxRebuys(2);
        room.setStatus(RoomStatus.WAITING);

        RoomPlayer player = new RoomPlayer();
        player.setChipsBalance(200);
        player.setRebuyCount(0);
        player.setInHand(false);

        BuyInBehavior behavior = factoryResolver.getFactory(GameMode.CASH).getBuyInBehavior();

        // 1ra recompra
        assertTrue(behavior.canRebuy(room, player));
        behavior.executeRebuy(room, player);
        assertEquals(1200, player.getChipsBalance());
        assertEquals(1, player.getRebuyCount());

        // 2da recompra
        assertTrue(behavior.canRebuy(room, player));
        behavior.executeRebuy(room, player);
        assertEquals(2200, player.getChipsBalance());
        assertEquals(2, player.getRebuyCount());

        // 3ra recompra (debe fallar debido a maxRebuys = 2)
        assertFalse(behavior.canRebuy(room, player));
    }

    @Test
    void testTournamentBuyInBehavior_Constraints() {
        Room room = new Room();
        room.setGameMode(GameMode.TOURNAMENT);
        room.setInitialChips(1000);
        room.setMaxRebuys(1);
        room.setSmallBlindAmount(10); // Nivel 1
        room.setStatus(RoomStatus.PLAYING);

        RoomPlayer player = new RoomPlayer();
        player.setChipsBalance(500); // Tiene fichas
        player.setRebuyCount(0);
        player.setInHand(true);

        BuyInBehavior behavior = factoryResolver.getFactory(GameMode.TOURNAMENT).getBuyInBehavior();

        // No puede recomprar porque tiene fichas > 0
        assertFalse(behavior.canRebuy(room, player));

        // Se queda con 0 fichas
        player.setChipsBalance(0);
        assertTrue(behavior.canRebuy(room, player));

        // Hace recompra
        behavior.executeRebuy(room, player);
        assertEquals(1000, player.getChipsBalance());
        assertEquals(1, player.getRebuyCount());
        assertTrue(player.getInHand());

        // Intenta recomprar de nuevo (límite superado)
        player.setChipsBalance(0);
        assertFalse(behavior.canRebuy(room, player));

        // Probar límite de nivel de ciegas (> 40)
        room.setSmallBlindAmount(80); // Nivel 4
        player.setRebuyCount(0); // Reiniciar para aislar variable
        assertFalse(behavior.canRebuy(room, player));
    }
}
