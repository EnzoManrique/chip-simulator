package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RiverState extends AbstractBettingPhaseState {

    @Override
    public BettingPhase getPhaseType() {
        return BettingPhase.RIVER;
    }

    @Override
    public void completeRound(Room room, List<RoomPlayer> orderedPlayers) {
        // En el River la siguiente fase siempre es Showdown
        room.setPhase(BettingPhase.SHOWDOWN);
        room.setHighestBet(0);
        // No hay más turnos de apuestas en el Showdown
    }
}
