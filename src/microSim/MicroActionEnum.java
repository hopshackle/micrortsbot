package microSim;

import hopshackle.simulation.*;
import rts.*;
import rts.units.*;

/*
This is intended to be a simple wrapper around rts.UnitAction
 */
public class MicroActionEnum implements ActionEnum<MicroAgent> {

    public enum ActionType {
        NONE,
        MOVE,
        HARVEST,
        RETURN,
        PRODUCE,
        ATTACK;
    }

    private static ActionType[] rtsTypeToOurs = {ActionType.NONE, ActionType.MOVE, ActionType.HARVEST,
            ActionType.RETURN, ActionType.PRODUCE, ActionType.ATTACK};

    private ActionType type;
    private UnitAction underlyingUnitAction;

    public MicroActionEnum(UnitAction ua) {
        type = rtsTypeToOurs[ua.getType()];
        underlyingUnitAction = ua;
    }

    public int ETA(MicroAgent unit) {
        return underlyingUnitAction.ETA(unit.getUnit());
    }

    public UnitAction getUnitAction() {
        return underlyingUnitAction;
    }

    public PlayerAction getPlayerAction(Unit u, GameState gs) {
        PlayerAction retValue = new PlayerAction();
        retValue.addUnitAction(u, underlyingUnitAction);
 //       retValue.setResourceUsage(underlyingUnitAction.resourceUsage(u, gs.getPhysicalGameState()));
        return retValue;
    }

    @Override
    public boolean equals(Object other) {
        if (underlyingUnitAction != null && other instanceof MicroActionEnum) {
            MicroActionEnum o = (MicroActionEnum) other;
            return (underlyingUnitAction.equals(o.underlyingUnitAction));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return underlyingUnitAction.hashCode();
    }

    @Override
    public String toString() {
        return underlyingUnitAction.toString();
    }

    @Override
    public String getChromosomeDesc() {
        return "MicroRTS";
    }

    @Override
    public MicroAction getAction(MicroAgent microAgent) {
        return new MicroAction(microAgent, this);
    }

    @Override
    public boolean isChooseable(MicroAgent microAgent) {
        return true;
        // not used
    }

    @Override
    public Enum<?> getEnum() {
        return type;
    }
}
