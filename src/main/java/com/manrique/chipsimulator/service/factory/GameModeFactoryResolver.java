package com.manrique.chipsimulator.service.factory;

import com.manrique.chipsimulator.model.enums.GameMode;
import org.springframework.stereotype.Component;

@Component
public class GameModeFactoryResolver {

    private final CashGameModeFactory cashGameModeFactory;
    private final TournamentModeFactory tournamentModeFactory;

    public GameModeFactoryResolver(CashGameModeFactory cashGameModeFactory, TournamentModeFactory tournamentModeFactory) {
        this.cashGameModeFactory = cashGameModeFactory;
        this.tournamentModeFactory = tournamentModeFactory;
    }

    public GameModeFactory getFactory(GameMode mode) {
        if (mode == GameMode.TOURNAMENT) {
            return tournamentModeFactory;
        }
        return cashGameModeFactory;
    }
}
