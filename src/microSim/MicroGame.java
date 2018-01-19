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
    private World world = new World();

    public MicroGame(GameState gs, int currentPlayer) {
        underlyingGameState = gs;
        unitIDToActorNumber = new HashMap();
        allActors = new ArrayList();
        for (Unit u : underlyingGameState.getUnits()) {
            allActors.add(new MicroAgent(world, this, u));
            unitIDToActorNumber.put(u.getID(), nextUnitNumber);
            actorNumberToPlayer.put(nextUnitNumber, u.getPlayer());
            nextUnitNumber++;
        }
        orderOfAction = sortPlayersIntoDecisionOrder(currentPlayer);
        currentActorIndex = 0;
        if (orderOfAction.isEmpty()) {
            throw new AssertionError("No actors to act in gameState");
        }
    }

    /*
    Sorts all ActorNumbers into the order in which they should decide. This starts with all Actors linked
    to the current player, and then those for the opponent.
     */
    private List<Integer> sortPlayersIntoDecisionOrder(int currentPlayer) {
        List<Integer> playerNumbersInUse = new ArrayList();
        for (int i : actorNumberToPlayer.keySet()) {
            playerNumbersInUse.add(i);
        }
        Collections.sort(playerNumbersInUse, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int differentPlayers = actorNumberToPlayer.get(o1) - actorNumberToPlayer.get(o2);
                switch (differentPlayers) {
                    case 0: // is usual order within a player
                        return o1 - o2;
                    case 1:
                    case -1:
                        if (actorNumberToPlayer.get(o1) == currentPlayer)
                            return -1;
                        if (actorNumberToPlayer.get(o2) == currentPlayer)
                            return 1;
                    default:
                        throw new AssertionError("Unexpected player difference for " + actorNumberToPlayer.get(o1)  + " against " + actorNumberToPlayer.get(o2));
                }
            }
        });
        return playerNumbersInUse;
    }

    public GameState getGameState() {
        return underlyingGameState;
    }

    @Override
    public Game<MicroAgent, MicroActionEnum> clone(MicroAgent perspectivePlayer) {
        return null;
        // TODO: implement
    }

    @Override
    public String getRef() {
        return String.valueOf(id);
    }

    @Override
    public MicroAgent getCurrentPlayer() {
        int actorNumber = getCurrentPlayerNumber();
        return allActors.get(actorNumber);
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
        return orderOfAction.get(currentActorIndex);
    }

    @Override
    public List<ActionEnum<MicroAgent>> getPossibleActions(MicroAgent player) {
        List<ActionEnum<MicroAgent>> retValue = new ArrayList();
        Unit u = player.getUnit();
        List<PlayerAction> actions = underlyingGameState.getPlayerActionsSingleUnit(u.getPlayer(), u);
        for (PlayerAction action : actions) {
            UnitAction ua = action.getAction(u);
            retValue.add(new MicroActionEnum(ua));
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
        Hence new Units not previously seen before are ascribed new actorNumbers, without chnagign the old ones.
        If a unit has died, then we will simply not see it again - so there is no need to remove its MicroAgent.
         */
    }
}
