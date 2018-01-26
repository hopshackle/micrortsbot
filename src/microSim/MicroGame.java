package microSim;

import hopshackle.simulation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import rts.*;
import rts.units.Unit;

/* A wrapper for GameState
 */
public class MicroGame extends Game<MicroAgent, MicroActionEnum> {

    private static AtomicInteger idFountain = new AtomicInteger(1);
    private int id = idFountain.getAndIncrement();
    private GameState underlyingGameState;
    private Map<Long, Integer> unitIDToActorNumber;
    private Map<Integer, Long> actorToUnitID;
    private List<Integer> orderOfAction;
    private int currentActorIndex;
    private int nextActorNumber = 1;

    public MicroGame(GameState gs) {
        underlyingGameState = gs;
        unitIDToActorNumber = new HashMap();
        actorToUnitID = new HashMap();
        setUpMasters();
        resetCurrentActors();
        scoreCalculator = new MicroGameScorer();
        if (debug) {
            log(String.format("MicroGame created with %d starting units", nextActorNumber - 1));
        }
    }

    private void setUpMasters() {
        // we hardcode two masters, but do not add them to players - as for the moment they never act
        for (int i = 0; i < 2; i++) {
            MicroAgent player = new MicroAgent(this, null);
            masters.add(player);
        }
    }

    private MicroGame(MicroGame master) {
        underlyingGameState = master.underlyingGameState.clone();
        currentActorIndex = master.currentActorIndex;
        nextActorNumber = master.nextActorNumber;
        unitIDToActorNumber = HopshackleUtilities.cloneMap(master.unitIDToActorNumber);
        actorToUnitID = HopshackleUtilities.cloneMap(master.actorToUnitID);
        setUpMasters();
        for (MicroAgent p : master.players) {
            long id = p.getUnit().getID();
            MicroAgent newAgent = new MicroAgent(this, underlyingGameState.getPhysicalGameState().getUnit(id));
            players.add(newAgent);
            MicroAgent masterAgent = masters.get(p.getUnit().getPlayer());
            masterAgents.put(newAgent, masterAgent);
        }
        scoreCalculator = master.scoreCalculator;
        resetCurrentActors();
    }


    public GameState getGameState() {
        return underlyingGameState;
    }

    @Override
    public Game<MicroAgent, MicroActionEnum> clone(MicroAgent perspectivePlayer) {
        return new MicroGame(this);
    }

    @Override
    public String getRef() {
        return String.valueOf(id);
    }

    @Override
    public MicroAgent getCurrentPlayer() {
        int actorNumber = getCurrentPlayerNumber();
        return players.get(actorNumber - 1);
    }

    @Override
    public int getCurrentPlayerNumber() {
        if (orderOfAction == null)
            return 1;
        return orderOfAction.get(currentActorIndex);
    }

    public List<Integer> getDecisionOrder() {
        return HopshackleUtilities.cloneList(orderOfAction);
    }

    @Override
    public List<ActionEnum<MicroAgent>> getPossibleActions(MicroAgent player) {
        List<ActionEnum<MicroAgent>> retValue = new ArrayList();
        Unit u = player.getUnit();
        List<PlayerAction> actions = underlyingGameState.getPlayerActionsSingleUnit(u.getPlayer(), u);
        if (actions.size() == 1 && actions.get(0).isEmpty()) {
            // this is an empty playerAction, which means that this unit is executing an action
            // and cannot make another decision
            return retValue;
        }
        for (PlayerAction action : actions) {
            UnitAction ua = action.getAction(u);
            retValue.add(new MicroActionEnum(ua));
        }
        if (debug) {
            StringBuilder message = new StringBuilder(String.format("Possible actions for actor %d : %s are:\n", getPlayerNumber(player), player.toString()));
            for (PlayerAction action : actions) {
                message.append(String.format("\t%s (%s)\n", action.toString(), action.getResourceUsage()));
            }
            log(message.toString());
        }
        return retValue;
    }

    @Override
    public boolean gameOver() {
        return underlyingGameState.gameover();
    }

    @Override
    public void updateGameStatus() {
        /*
        This will be called by Game every time an action has been completed (i.e. a decision made).
        We therefore need to update the next actor.

        If we have reached the end of all the actors who have decisions to make, then we instead
        cycle forward the GameState to a point at which another decision is required. This
        will also reset the list of which actors need to make a decision.
        However, we want to retain a firm link between actorNumber and the Unit.ID.
        Hence new Units not previously seen before are ascribed new actorNumbers, without changing the old ones.
        If a unit has died, then we will simply not see it again - so there is no need to remove its MicroAgent.
         */

        currentActorIndex++;
        if (currentActorIndex >= orderOfAction.size()) {
            // we have finished all decisions
            boolean gameOver = false;
            do {
                gameOver = underlyingGameState.cycle();
                setTime(underlyingGameState.getTime());
                if (debug) {
                    log(String.format("Moving time on by one tick to %d", underlyingGameState.getTime()));
                    log(String.format("GameState ResourceUsage: %s", getGameState().getResourceUsage().toString()));
                }
            } while (!gameOver && underlyingGameState.getNextChangeTime() > underlyingGameState.getTime());
            if (!gameOver) resetCurrentActors();
        } else {
            // no need to do anything other than increment the currentActorIndex
            if (debug) {
                int actorNumber = orderOfAction.get(currentActorIndex);
                log(String.format("Next decision from %d : %s", currentActorIndex, players.get(actorNumber - 1)));
            }
        }
    }

    private void resetCurrentActors() {
        for (Unit u : underlyingGameState.getUnits()) {
            if (u.getPlayer() == -1) continue;  // a resource, or non-player controlled thingy
            if (!unitIDToActorNumber.containsKey(u.getID())) {
                // a new unit that we need to track
                MicroAgent newAgent = new MicroAgent(this, u);
                players.add(newAgent);
                MicroAgent master = masters.get(u.getPlayer());
                masterAgents.put(newAgent, master);
                // then set parent - this will trigger a BIRTH event
                newAgent.addParent(master);
                // uses convention that masters contains the two Players in correct order
                unitIDToActorNumber.put(u.getID(), nextActorNumber);
                actorToUnitID.put(nextActorNumber, u.getID());
                if (debug) {
                    log(String.format("Added new actor %d : %s", nextActorNumber, getPlayer(nextActorNumber)));
                }
                nextActorNumber++;
            }
        }
        orderOfAction = sortPlayersIntoDecisionOrder(0);
        currentActorIndex = 0;
        if (orderOfAction.isEmpty()) {
            throw new AssertionError("No actors to act in gameState");
        }
    }

    /*
Sorts all ActorNumbers into the order in which they should decide. This starts with all Actors linked
to the current player, and then those for the opponent.
 */
    public static List<Integer> sortPlayersIntoDecisionOrder(GameState gs, int playerID) {
        MicroGame temp = new MicroGame(gs);
        return temp.sortPlayersIntoDecisionOrder(playerID);
    }
    public List<Integer> sortPlayersIntoDecisionOrder(int playerID) {
        List<Integer> actorNumbersInUse = new ArrayList();
        List<Unit> activeUnits = underlyingGameState.getUnits();
        boolean mainDebug = debug;
        debug = false;
        for (Unit activeUnit : activeUnits) {
            if (activeUnit.getPlayer() == -1) continue;  // a resource, or non-player controlled thingy
            long id = activeUnit.getID();
            int actorNumber = unitIDToActorNumber.get(id);
            MicroAgent nextActor = players.get(actorNumber-1);
            if (!getPossibleActions(nextActor).isEmpty())
                actorNumbersInUse.add(actorNumber);
        }
        debug = mainDebug;

        Collections.sort(actorNumbersInUse, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int differentPlayers = getPlayerForActor(o1) - getPlayerForActor(o2);
                switch (differentPlayers) {
                    case 0: // is usual order within a player
                        return o1 - o2;
                    case 1:
                    case -1:
                        // we want all units for playerID first in the array
                        int retValue = playerID == 0 ? differentPlayers : -differentPlayers;
                        return retValue;
                    default:
                        throw new AssertionError("Unexpected player difference for " + getPlayerForActor(o1) + " against " + getPlayerForActor(o2));
                }
            }
        });
        return actorNumbersInUse;
    }

    @Override
    public String toString() {
        return "MicroGame_" + String.valueOf(id);
    }

    public long getUnitIDForAgent(int ref) {
        return actorToUnitID.get(ref);
    }
    public int getPlayerForActor(int ref){
        MicroAgent master = getMasterOf(players.get(ref-1));
        return masters.indexOf(master);
    }
}
