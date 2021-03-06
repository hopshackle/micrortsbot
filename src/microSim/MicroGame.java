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
    private Map<Integer, Integer> actorNumberToPlayer;
    private List<Integer> orderOfAction;
    private List<MicroAgent> allActors;
    private int currentActorIndex;
    private int nextUnitNumber = 1;

    public MicroGame(GameState gs) {
        underlyingGameState = gs;
        unitIDToActorNumber = new HashMap();
        actorNumberToPlayer = new HashMap();
        allActors = new ArrayList();
        resetCurrentActors();
        scoreCalculator = new MicroGameScorer();
        if (debug) {
            log(String.format("MicroGame created with %d starting units", nextUnitNumber - 1));
        }
    }

    private MicroGame(MicroGame master) {
        underlyingGameState = master.underlyingGameState.clone();
        currentActorIndex = master.currentActorIndex;
        nextUnitNumber = master.nextUnitNumber;
        unitIDToActorNumber = HopshackleUtilities.cloneMap(master.unitIDToActorNumber);
        actorNumberToPlayer = HopshackleUtilities.cloneMap(master.actorNumberToPlayer);
        allActors = new ArrayList();
        for (MicroAgent p : master.allActors) {
            long id = p.getUnit().getID();
            MicroAgent newAgent = new MicroAgent(this, underlyingGameState.getPhysicalGameState().getUnit(id));
            allActors.add(newAgent);
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
        return allActors.get(actorNumber - 1);
    }

    @Override
    public List<MicroAgent> getAllPlayers() {
        return allActors;
    }

    @Override
    public int getPlayerNumber(MicroAgent player) {
        int actorNumber = allActors.indexOf(player);
        return actorNumber + 1;
    }

    @Override
    public MicroAgent getPlayer(int n) {
        return allActors.get(n - 1);
    }

    @Override
    public int getCurrentPlayerNumber() {
        if (orderOfAction == null)
            return 1;
        return orderOfAction.get(currentActorIndex);
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
                log(String.format("Next decision from %d : %s", currentActorIndex, allActors.get(actorNumber - 1)));
            }
        }
    }

    private void resetCurrentActors() {
        for (Unit u : underlyingGameState.getUnits()) {
            if (u.getPlayer() == -1) continue;  // a resource, or non-player controlled thingy
            if (!unitIDToActorNumber.containsKey(u.getID())) {
                // a new unit that we need to track
                allActors.add(new MicroAgent(this, u));
                unitIDToActorNumber.put(u.getID(), nextUnitNumber);
                actorNumberToPlayer.put(nextUnitNumber, u.getPlayer());
                if (debug) {
                    log(String.format("Added new actor %d : %s", nextUnitNumber, getPlayer(nextUnitNumber)));
                }
                nextUnitNumber++;
            }
        }
        orderOfAction = sortPlayersIntoDecisionOrder();
        currentActorIndex = 0;
        if (orderOfAction.isEmpty()) {
            throw new AssertionError("No actors to act in gameState");
        }
    }

    /*
Sorts all ActorNumbers into the order in which they should decide. This starts with all Actors linked
to the current player, and then those for the opponent.
 */
    private List<Integer> sortPlayersIntoDecisionOrder() {
        List<Integer> actorNumbersInUse = new ArrayList();
        List<Unit> activeUnits = underlyingGameState.getUnits();
        boolean mainDebug = debug;
        debug = false;
        for (Unit activeUnit : activeUnits) {
            if (activeUnit.getPlayer() == -1) continue;  // a resource, or non-player controlled thingy
            long id = activeUnit.getID();
            int actorNumber = unitIDToActorNumber.get(id);
            if (!getPossibleActions(allActors.get(actorNumber - 1)).isEmpty())
                actorNumbersInUse.add(actorNumber);
        }
        debug = mainDebug;      // to switch off logging when we call getPossibleActions()

        Collections.sort(actorNumbersInUse, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int differentPlayers = actorNumberToPlayer.get(o1) - actorNumberToPlayer.get(o2);
                switch (differentPlayers) {
                    case 0: // is usual order within a player
                        return o1 - o2;
                    case 1:
                    case -1:
                        return differentPlayers;
                    default:
                        throw new AssertionError("Unexpected player difference for " + actorNumberToPlayer.get(o1) + " against " + actorNumberToPlayer.get(o2));
                }
            }
        });
        return actorNumbersInUse;
    }

    @Override
    public String toString() {
        return "MicroGame_" + String.valueOf(id);
    }
}
