package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShowdownState extends AbstractBettingPhaseState {

    @Override
    public BettingPhase getPhaseType() {
        return BettingPhase.SHOWDOWN;
    }

    @Override
    public void completeRound(Room room, List<RoomPlayer> orderedPlayers) {
        // En Showdown no se transiciona automáticamente de ronda de apuestas.
        // La mano debe ser resuelta por el administrador a través del endpoint de fin de mano.
    }
}
