package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.EndHandRequestDTO;
import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowdownService {

    public void endHand(Room room, EndHandRequestDTO request) {
        room.setPhase(BettingPhase.SHOWDOWN);

        for (Pot pot : room.getPots()) {
            List<RoomPlayer> eligibleWinners = pot.getEligiblePlayers().stream()
                    .filter(p -> request.winnerUsernames().contains(p.getUser().getUsername()))
                    .toList();

            if (!eligibleWinners.isEmpty()) {
                int splitAmount = pot.getAmount() / eligibleWinners.size();
                int extraChips = pot.getAmount() % eligibleWinners.size();

                for (int i = 0; i < eligibleWinners.size(); i++) {
                    RoomPlayer winner = eligibleWinners.get(i);
                    int amountToAdd = splitAmount;
                    if (i == 0) {
                        amountToAdd += extraChips;
                    }
                    winner.setChipsBalance(winner.getChipsBalance() + amountToAdd);
                }
            }
        }
        
        room.getPots().clear();
    }

    /**
     * Finaliza automáticamente la mano cuando solo queda 1 jugador.
     * Le da todo el pozo a ese jugador.
     */
    public void endHandAuto(Room room, RoomPlayer winner) {
        room.setPhase(BettingPhase.SHOWDOWN);

        // Un solo winner - le da todo el pozo
        for (Pot pot : room.getPots()) {
            if (pot.getAmount() > 0) {
                winner.setChipsBalance(winner.getChipsBalance() + pot.getAmount());
            }
        }

        room.getPots().clear();
    }
}
