package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.EndHandRequestDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import com.manrique.chipsimulator.service.strategy.PotDistributionStrategy;
import com.manrique.chipsimulator.service.strategy.SingleWinnerStrategy;
import com.manrique.chipsimulator.service.strategy.SplitPotStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowdownService {

    private final PotDistributionStrategy singleWinnerStrategy;
    private final PotDistributionStrategy splitPotStrategy;

    public ShowdownService(SingleWinnerStrategy singleWinnerStrategy, SplitPotStrategy splitPotStrategy) {
        this.singleWinnerStrategy = singleWinnerStrategy;
        this.splitPotStrategy = splitPotStrategy;
    }

    public void endHand(Room room, EndHandRequestDTO request) {
        room.setPhase(BettingPhase.SHOWDOWN);

        for (Pot pot : room.getPots()) {
            List<RoomPlayer> eligibleWinners = pot.getEligiblePlayers().stream()
                    .filter(p -> request.winnerUsernames().contains(p.getUser().getUsername()))
                    .toList();

            if (!eligibleWinners.isEmpty()) {
                PotDistributionStrategy strategy = getStrategy(eligibleWinners.size());
                strategy.distribute(pot, eligibleWinners);
            }
        }
        
        room.getPots().clear();
    }

    /**
     * Finaliza automáticamente la mano cuando solo queda 1 jugador.
     * Le da todo el pozo a ese jugador utilizando la estrategia SingleWinnerStrategy.
     */
    public void endHandAuto(Room room, RoomPlayer winner) {
        room.setPhase(BettingPhase.SHOWDOWN);

        for (Pot pot : room.getPots()) {
            if (pot.getAmount() > 0) {
                singleWinnerStrategy.distribute(pot, List.of(winner));
            }
        }

        room.getPots().clear();
    }

    private PotDistributionStrategy getStrategy(int winnersCount) {
        if (winnersCount == 1) {
            return singleWinnerStrategy;
        } else {
            return splitPotStrategy;
        }
    }
}
