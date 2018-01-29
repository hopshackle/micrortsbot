package microSim;

import ai.core.AI;
import hopshackle.simulation.*;
import rts.*;
import rts.units.*;
import java.util.*;

public class AIDecider extends BaseStateDecider<MicroAgent> {

    private AI underlyingBrain;

    public AIDecider(AI underlyingAI) {
        super(new MicroGameStateFactory());
        underlyingBrain = underlyingAI;
    }

    protected ActionEnum<MicroAgent> selectOption(List<ActionEnum<MicroAgent>> optionList, MicroAgent decidingAgent) {
        GameState gs = decidingAgent.getGameState();
        Unit unit = decidingAgent.getUnit();
        int playerID = decidingAgent.getUnit().getPlayer();
        // we assume, but never check, the chosen option by the heuristic is in optionList
        try {
            PlayerAction pa = underlyingBrain.getAction(playerID, gs);
            UnitAction ua = pa.getAction(unit);
            MicroActionEnum retValue = new MicroActionEnum(ua);
            return retValue;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Unhandled Exception " + e.toString());
        }
    }

    @Override
    public double valueOption(ActionEnum<MicroAgent> option, State<MicroAgent> state) {
        return 0;
    }

    @Override
    public void learnFrom(ExperienceRecord<MicroAgent> exp, double maxResult) {
        // nothing to learn
    }
}
