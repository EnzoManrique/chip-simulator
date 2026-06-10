package com.manrique.chipsimulator.service.state;

import com.manrique.chipsimulator.model.enums.BettingPhase;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BettingPhaseStateFactory {

    private final Map<BettingPhase, BettingPhaseState> states = new HashMap<>();

    public BettingPhaseStateFactory(List<BettingPhaseState> stateList) {
        for (BettingPhaseState state : stateList) {
            states.put(state.getPhaseType(), state);
        }
    }

    public BettingPhaseState getState(BettingPhase phase) {
        if (phase == null) {
            return states.get(BettingPhase.PRE_FLOP);
        }
        BettingPhaseState state = states.get(phase);
        if (state == null) {
            throw new IllegalArgumentException("No state implementation found for phase: " + phase);
        }
        return state;
    }
}
