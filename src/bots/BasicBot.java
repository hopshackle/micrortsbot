package bots;

import ai.core.*;
import rts.*;
import rts.units.*;
import util.*;

import java.util.*;

public class BasicBot extends AIWithComputationBudget {

    public BasicBot(int mt, int mi)  {
        super(mt, mi);
    }

    public BasicBot() {
        super(100, 0);
    }

    @Override
    public void reset() {
        // reset between games I assume
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        try {
            if (!gs.canExecuteAnyAction(player)) return new PlayerAction();
            PlayerActionGenerator pag = new PlayerActionGenerator(gs, player);
            for (Pair<Unit,List<UnitAction>> option : pag.getChoices()) {
                System.out.println(option.toString());
            }
            return pag.getRandom();
        }catch(Exception e) {
            // The only way the player action generator returns an exception is if there are no units that
            // can execute actions, in this case, just return an empty action:
            // However, this should never happen, since we are checking for this at the beginning
            return new PlayerAction();
        }
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

