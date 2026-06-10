package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PreFlopState extends AbstractBettingPhaseState {

    @Override
    public BettingPhase getPhaseType() {
        return BettingPhase.PRE_FLOP;
    }

    @Override
    public void completeRound(Room room, List<RoomPlayer> orderedPlayers) {
        advanceStandardPhase(room, orderedPlayers, BettingPhase.FLOP);
    }
}
