package bots;

import ai.core.*;
import hopshackle.simulation.HopshackleUtilities;
import microSim.MicroGame;
import rts.*;
import rts.units.*;
import util.*;

import java.util.*;

public class BasicBot extends AIWithComputationBudget {

    public BasicBot(int mt, int mi) {
        super(mt, mi);
    }

    public BasicBot(UnitTypeTable utt) {
        super(100, 0);
    }

    @Override
    public void reset() {
        // reset between games I assume
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (!gs.canExecuteAnyAction(player)) return new PlayerAction();
        PlayerActionGenerator pag = new PlayerActionGenerator(gs, player);
        for (Pair<Unit, List<UnitAction>> option : pag.getChoices()) {
            System.out.println(option.toString());
        }

        long start  = System.currentTimeMillis();
        MicroGame mg = new MicroGame(gs.clone());
        double[] result = mg.playGame();
        System.out.println("Time taken: " + (System.currentTimeMillis() - start));
        System.out.println(HopshackleUtilities.formatArray(result, ", ", "%.2f"));

        return pag.getRandom();
    }

    @Override
    public AI clone() {
        // provides a copy of the AI with the same configuration (no need for full copy of internal state)
        return new BasicBot(this.TIME_BUDGET, this.ITERATIONS_BUDGET);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public String statisticsString() {
        // anything useful to be printed at the end of each game
        return "";
    }

    @Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
        // used for what it says on the tin
    }

}

