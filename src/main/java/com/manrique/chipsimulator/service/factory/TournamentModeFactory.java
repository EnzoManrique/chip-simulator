package com.manrique.chipsimulator.service.factory;

import org.springframework.stereotype.Component;

@Component
public class TournamentModeFactory implements GameModeFactory {

    private final BlindStructure blindStructure = new TournamentBlindStructure();
    private final BuyInBehavior buyInBehavior = new TournamentBuyInBehavior();

    @Override
    public BlindStructure getBlindStructure() {
        return blindStructure;
    }

    @Override
    public BuyInBehavior getBuyInBehavior() {
        return buyInBehavior;
    }
}
