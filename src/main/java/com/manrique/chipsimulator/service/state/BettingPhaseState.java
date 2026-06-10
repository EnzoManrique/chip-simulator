package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import java.util.List;

public interface BettingPhaseState {
    BettingPhase getPhaseType();
    void completeRound(Room room, List<RoomPlayer> orderedPlayers);
}
