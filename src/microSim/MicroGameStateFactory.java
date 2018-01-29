package microSim;

import hopshackle.simulation.*;

import java.util.ArrayList;
import java.util.List;

public class MicroGameStateFactory implements StateFactory<MicroAgent> {

    @Override
    public State<MicroAgent> getCurrentState(MicroAgent agent) {
        MicroGame game = (MicroGame) agent.getGame();
        int playerID = game.getPlayerForActor(game.getPlayerNumber(agent));
        return new MicroGameState(game.getGameState().clone(), playerID);
    }

    @Override
    public <V extends GeneticVariable<MicroAgent>> List<V> getVariables() {
        return new ArrayList();
    }

    @Override
    public StateFactory<MicroAgent> cloneWithNewVariables(List<GeneticVariable<MicroAgent>> newVar) {
        return this;
    }
}
