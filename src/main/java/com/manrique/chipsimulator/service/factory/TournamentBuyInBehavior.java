package com.manrique.chipsimulator.service.factory;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;

public class TournamentBuyInBehavior implements BuyInBehavior {
    @Override
    public boolean canRebuy(Room room, RoomPlayer player) {
        // Solo si se quedó sin fichas (o por debajo de 0)
        if (player.getChipsBalance() > 0) {
            return false;
        }
        // Límite de recompras
        if (room.getMaxRebuys() != null && player.getRebuyCount() >= room.getMaxRebuys()) {
            return false;
        }
        // Solo en fases tempranas (por defecto ciega pequeña <= 40)
        int currentSB = room.getSmallBlindAmount() != null ? room.getSmallBlindAmount() : 10;
        if (currentSB > 40) {
            return false;
        }
        return true;
    }

    @Override
    public void executeRebuy(Room room, RoomPlayer player) {
        int amount = getRebuyAmount(room);
        player.setChipsBalance(amount);
        player.setRebuyCount(player.getRebuyCount() + 1);
        player.setInHand(true); // Reincorporarlo
        player.setIsAllIn(false);
    }

    @Override
    public int getRebuyAmount(Room room) {
        return room.getInitialChips() != null ? room.getInitialChips() : 1000;
    }
}
