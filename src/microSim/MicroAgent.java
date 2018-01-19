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

    public MicroAgent(World world, MicroGame game, Unit unit) {
        super(world);
        this.unit = unit;
        this.game = game;
    }

    public Unit getUnit() {
        return unit;
    }

    public GameState getGameState() {
        return game.getGameState();
    }
}
