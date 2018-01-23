package microSim;

import hopshackle.simulation.*;
import rts.*;
import rts.units.*;

/*
A Wrapper for rts.Unit
 */
public class MicroAgent extends Agent {

    private Unit unit;
    private MicroGame game;
    private static OpenLoopStateFactory<MicroAgent> openLoopFactory = OpenLoopStateFactory.newInstanceGameLevelStates();

    public MicroAgent(MicroGame game, Unit unit) {
        super(game.getWorld());
        this.unit = unit;
        this.game = game;
        setDecider(new RandomDecider(openLoopFactory));
    }

    public MicroAgent clone(MicroGame g) {
        Unit clonedUnit = getGameState().getUnit(this.unit.getID());
        return new MicroAgent(g, clonedUnit);
    }

    public Unit getUnit() {
        return unit;
    }

    public GameState getGameState() {
        return game.getGameState();
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public String toString() {
        return String.format("%s [%d]", unit.toString(), getUniqueID());
    }

}
