package microSim;

import hopshackle.simulation.*;
import rts.*;

import java.util.concurrent.atomic.AtomicLong;

public class MicroGameState implements State<MicroAgent> {

    private static AtomicLong idFountain = new AtomicLong(1);
    private long id;
    private GameState underlyingGameState;
    private int playerID;

    public MicroGameState(GameState gs, int playerID) {
        underlyingGameState = gs;
        this.playerID = playerID;
        id = idFountain.getAndIncrement();
    }

    @Override
    public double[] getAsArray() {
        // TODO: Later this can be set up using GeneticVariables applied to the underlyingGameState
        return new double[0];
    }

    @Override
    public int getActorRef() {
        return playerID;
    }

    @Override
    public double[] getScore() {
        // we could in the future use an evaluation function to value the underlyingGameState here
        return new double[] {0.0, 0.0};
    }

    @Override
    public String getAsString() {
        return String.valueOf(id);
    }

    @Override
    public State<MicroAgent> apply(ActionEnum<MicroAgent> proposedAction) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public State<MicroAgent> clone() {
        return this;
    }
}
