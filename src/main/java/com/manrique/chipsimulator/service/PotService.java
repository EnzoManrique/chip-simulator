package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.model.Pot;
import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.RoomPlayer;
import com.manrique.chipsimulator.repository.PotRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PotService {

    private final PotRepository potRepository;

    public PotService(PotRepository potRepository) {
        this.potRepository = potRepository;
    }

    /**
     * Algoritmo principal para recolectar las apuestas realizadas en la ronda y
     * dividirlas de forma precisa en pozos principal y secundarios (Side Pots).
     */
    public void collectBetsAndBuildPots(Room room, List<RoomPlayer> players) {
        List<RoomPlayer> contributors = players.stream()
                .filter(p -> p.getCurrentBet() != null && p.getCurrentBet() > 0)
                .toList();

        if (contributors.isEmpty()) {
            return;
        }

        while (true) {
            // Encontrar la menor apuesta restante mayor a 0 en esta ronda
            int minBet = Integer.MAX_VALUE;
            for (RoomPlayer p : contributors) {
                if (p.getCurrentBet() > 0 && p.getCurrentBet() < minBet) {
                    minBet = p.getCurrentBet();
                }
            }

            if (minBet == Integer.MAX_VALUE) {
                break; // Todas las apuestas de la ronda han sido procesadas
            }

            final int chunk = minBet;
            List<RoomPlayer> activeContributors = contributors.stream()
                    .filter(p -> p.getCurrentBet() >= chunk)
                    .toList();

            // Los jugadores elegibles para ganar esta porción son aquellos que contribuyen y siguen en la mano (inHand)
            List<RoomPlayer> eligiblePlayers = activeContributors.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getInHand()))
                    .toList();

            int potAmountCollected = chunk * activeContributors.size();

            // Si hay 1 o menos jugadores elegibles en esta porción, se les reembolsa el dinero recolectado
            if (eligiblePlayers.size() <= 1) {
                if (eligiblePlayers.size() == 1) {
                    RoomPlayer refundPlayer = eligiblePlayers.get(0);
                    refundPlayer.setChipsBalance(refundPlayer.getChipsBalance() + potAmountCollected);
                }
            } else {
                // Buscar si ya existe un pozo (Pot) en la sala con exactamente las mismas personas elegibles
                Pot matchingPot = null;
                for (Pot pot : room.getPots()) {
                    if (hasSameElements(pot.getEligiblePlayers(), eligiblePlayers)) {
                        matchingPot = pot;
                        break;
                    }
                }

                if (matchingPot != null) {
                    // Si coincide la elegibilidad, sumamos el dinero a ese pozo existente
                    matchingPot.setAmount(matchingPot.getAmount() + potAmountCollected);
                    potRepository.save(matchingPot);
                } else {
                    // Si la elegibilidad cambia (ej. debido a un jugador All-in), creamos un nuevo pozo secundario (Side Pot)
                    Pot newPot = Pot.builder()
                            .room(room)
                            .amount(potAmountCollected)
                            .eligiblePlayers(new ArrayList<>(eligiblePlayers))
                            .build();
                    newPot = potRepository.save(newPot);
                    room.getPots().add(newPot);
                }
            }

            // Descontar la porción procesada del saldo temporal de apuestas
            for (RoomPlayer p : activeContributors) {
                p.setCurrentBet(p.getCurrentBet() - chunk);
            }
        }

        // Asegurarse de que el currentBet temporal de todos quede en 0
        for (RoomPlayer p : players) {
            p.setCurrentBet(0);
        }
    }

    private boolean hasSameElements(List<RoomPlayer> list1, List<RoomPlayer> list2) {
        if (list1.size() != list2.size()) return false;
        return list1.containsAll(list2) && list2.containsAll(list1);
    }
}
