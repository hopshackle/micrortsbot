package microSim;

import hopshackle.simulation.*;
import rts.*;

/*
Combines MicroActionEnum and MicroAgent to generate a PlayerAction
When run(), this is issued to the underlying GameState (akin to World)
 */
public class MicroAction extends Action<MicroAgent> {

    public MicroAction(MicroActionEnum type, MicroAgent microAgent, long startOffset, long duration, boolean recordAction) {
        super(type, microAgent, startOffset, duration, recordAction);
    }

    public MicroAction(MicroAgent agent, MicroActionEnum actionEnum) {
        this(actionEnum, agent, 0, actionEnum.ETA(agent), false);
    }

    public void doStuff() {
        // Only when the MicroAction is executed do we incorporate the Assignment into the GameState
        MicroActionEnum type = (MicroActionEnum) getType();
        PlayerAction pa = type.getPlayerAction(actor.getUnit());
        actor.getGameState().issue(pa);
    }
}
