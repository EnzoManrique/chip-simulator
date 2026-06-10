package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class FlopState extends AbstractBettingPhaseState {

    @Override
    public BettingPhase getPhaseType() {
        return BettingPhase.FLOP;
    }

    @Override
    public void completeRound(Room room, List<RoomPlayer> orderedPlayers) {
        advanceStandardPhase(room, orderedPlayers, BettingPhase.TURN);
    }
}
