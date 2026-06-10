package com.manrique.chipsimulator.service.strategy;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.RoomPlayer;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SplitPotStrategy implements PotDistributionStrategy {

    @Override
    public void distribute(Pot pot, List<RoomPlayer> winners) {
        if (winners == null || winners.isEmpty()) {
            throw new IllegalArgumentException("SplitPotStrategy requiere al menos 1 ganador");
        }

        int splitAmount = pot.getAmount() / winners.size();
        int extraChips = pot.getAmount() % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            RoomPlayer winner = winners.get(i);
            int amountToAdd = splitAmount;
            if (i == 0) {
                amountToAdd += extraChips;
            }
            winner.setChipsBalance(winner.getChipsBalance() + amountToAdd);
        }
    }
}
