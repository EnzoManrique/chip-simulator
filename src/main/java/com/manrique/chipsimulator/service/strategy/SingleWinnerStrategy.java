package com.manrique.chipsimulator.service.strategy;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.RoomPlayer;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SingleWinnerStrategy implements PotDistributionStrategy {

    @Override
    public void distribute(Pot pot, List<RoomPlayer> winners) {
        if (winners == null || winners.size() != 1) {
            throw new IllegalArgumentException("SingleWinnerStrategy requiere exactamente 1 ganador");
        }
        RoomPlayer winner = winners.get(0);
        winner.setChipsBalance(winner.getChipsBalance() + pot.getAmount());
    }
}
