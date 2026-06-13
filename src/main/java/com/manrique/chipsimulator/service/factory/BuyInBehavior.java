package com.manrique.chipsimulator.service.factory;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;

public interface BuyInBehavior {
    boolean canRebuy(Room room, RoomPlayer player);
    void executeRebuy(Room room, RoomPlayer player);
    int getRebuyAmount(Room room);
}
