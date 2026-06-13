package com.manrique.chipsimulator.service.factory;

import org.springframework.stereotype.Component;

@Component
public class CashGameModeFactory implements GameModeFactory {

    private final BlindStructure blindStructure = new CashBlindStructure();
    private final BuyInBehavior buyInBehavior = new CashBuyInBehavior();

    @Override
    public BlindStructure getBlindStructure() {
        return blindStructure;
    }

    @Override
    public BuyInBehavior getBuyInBehavior() {
        return buyInBehavior;
    }
}
