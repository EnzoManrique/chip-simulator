package com.manrique.chipsimulator.service.factory;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.RoomStatus;

public class CashBuyInBehavior implements BuyInBehavior {
    @Override
    public boolean canRebuy(Room room, RoomPlayer player) {
        // No debe estar en medio de una mano activa en juego
        if (Boolean.TRUE.equals(player.getInHand()) && room.getStatus() == RoomStatus.PLAYING) {
            return false;
        }
        // Verificar límite de recompras (si maxRebuys es nulo es ilimitado)
        if (room.getMaxRebuys() != null && player.getRebuyCount() >= room.getMaxRebuys()) {
            return false;
        }
        return true;
    }

    @Override
    public void executeRebuy(Room room, RoomPlayer player) {
        int amount = getRebuyAmount(room);
        player.setChipsBalance(player.getChipsBalance() + amount);
        player.setRebuyCount(player.getRebuyCount() + 1);
        
        // Si no estaba en la mano (ej. porque se había quedado sin fichas en una mano previa)
        // lo activamos para que pueda jugar en la siguiente mano
        if (player.getChipsBalance() > 0) {
            player.setIsAllIn(false);
        }
    }

    @Override
    public int getRebuyAmount(Room room) {
        return room.getInitialChips() != null ? room.getInitialChips() : 1000;
    }
}
