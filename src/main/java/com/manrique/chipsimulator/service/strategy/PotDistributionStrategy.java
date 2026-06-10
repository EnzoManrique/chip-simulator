package com.manrique.chipsimulator.service.strategy;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.RoomPlayer;
import java.util.List;

public interface PotDistributionStrategy {
    void distribute(Pot pot, List<RoomPlayer> winners);
}
