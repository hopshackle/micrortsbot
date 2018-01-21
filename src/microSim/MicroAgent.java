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

    public MicroAgent(World world, MicroGame game, Unit unit) {
        super(world);
        this.unit = unit;
        this.game = game;
        setDecider(new RandomDecider(openLoopFactory));
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
