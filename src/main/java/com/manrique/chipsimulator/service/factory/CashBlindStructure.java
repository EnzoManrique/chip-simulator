package com.manrique.chipsimulator.service.factory;

import com.manrique.chipsimulator.model.Room;

public class CashBlindStructure implements BlindStructure {
    @Override
    public void setupBlindsForNextHand(Room room) {
        if (Boolean.TRUE.equals(room.getBlindsIncrease())) {
            room.setHandCount(room.getHandCount() + 1);
            int handsToIncrease = room.getHandsToIncrease() != null ? room.getHandsToIncrease() : 3;
            if (room.getHandCount() > 0 && room.getHandCount() % handsToIncrease == 0) {
                // Duplica el Small Blind
                int currentSB = room.getSmallBlindAmount() != null ? room.getSmallBlindAmount() : 10;
                room.setSmallBlindAmount(currentSB * 2);
            }
        }
    }
}
