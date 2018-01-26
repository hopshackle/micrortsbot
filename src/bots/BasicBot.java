package bots;

import ai.core.*;
import hopshackle.simulation.*;
import microSim.*;
import rts.*;
import rts.units.*;

import java.util.*;

public class BasicBot extends AIWithComputationBudget {

    private OpenLoopStateFactory<MicroAgent> stateFactory = OpenLoopStateFactory.newInstanceGameLevelStates();
    private BaseStateDecider<MicroAgent> rollout = new RandomDecider(stateFactory);
    private BaseStateDecider<MicroAgent> opponentModel = new RandomDecider(stateFactory);
    private MCTSMasterDecider<MicroAgent> decider = new MCTSMasterDecider(stateFactory, rollout, opponentModel);

    {
        decider.injectProperties(SimProperties.getDeciderProperties("GLOBAL"));
    }

    public BasicBot(int mt, int mi) {
        super(mt, mi);
    }

    public BasicBot(UnitTypeTable utt) {
        super(100, 0);
    }

    @Override
    public void reset() {
        // reset between games I assume
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (!gs.canExecuteAnyAction(player)) return new PlayerAction();

        long start = System.currentTimeMillis();
        MicroGame mg = new MicroGame(gs.clone());

        MicroAgent firstAgent = mg.getCurrentPlayer();  // actually actor

        stateFactory.reset();
        State<MicroAgent> currentState = stateFactory.getCurrentState(firstAgent);

        // we make the first decision...this also constructs the tree, that we then use to extract all moves
        decider.makeDecision(firstAgent, mg.getPossibleActions(firstAgent));

        PlayerAction retValue = extractPlayerActionFrom(player, decider.getTree(firstAgent), currentState, mg);
        mg.log(String.format("Full move chosen: %s", retValue.toString()));
        System.out.println("Time taken: " + (System.currentTimeMillis() - start));

        return retValue;
    }

    private PlayerAction extractPlayerActionFrom(int playerID, MonteCarloTree<MicroAgent> tree, State<MicroAgent> fromState, MicroGame game) {
        // once the time limit is up, I need to extract the full PlayerAction from the generated Tree.
        // This involves descending it until we reach a decision by another player
        // If there are still MicroAgents who have not decided, then pick randomly
        PlayerAction retValue = new PlayerAction();
        String stateAsString = fromState.getAsString();
        List<Integer> decisionOrder = game.getDecisionOrder();
        for (int actorRef : decisionOrder) {
            int masterRef = game.getPlayerForActor(actorRef);
            if (masterRef != playerID)
                continue;
            Unit u = game.getGameState().getUnit(game.getUnitIDForAgent(actorRef));
            if (u.getPlayer() != playerID)
                throw new AssertionError("Unit has incorrect player ID");
            MCStatistics<MicroAgent> stats = tree.getStatisticsFor(stateAsString);
            MicroActionEnum action;
            if (stats == null) {
                List<ActionEnum<MicroAgent>> possibleActions = game.getPossibleActions(game.getPlayer(actorRef));
                action = (MicroActionEnum) rollout.makeDecision(game.getPlayer(actorRef), possibleActions);
                // stateAsString remains unchanged - we are now beyond the end of the tree
            } else {
                action = (MicroActionEnum) stats.getBestAction(stats.getPossibleActions(), masterRef + 1);
                Map<String, Integer> successorStates = stats.getSuccessorStatesFrom(action);
                if (successorStates.keySet().size() > 1) {
                    throw new AssertionError("With OpenLoop we only expect 1 successor state, not " + successorStates.keySet().size());
                }
                for (String s : successorStates.keySet())
                    stateAsString = s;
            }

            PlayerAction playerAction = action.getPlayerAction(u, game.getGameState());
            // GameState can (I think) be the initial GameState before executing any actions
            // All it will not include are the other moves made...but that's covered in the Merge
            retValue = retValue.merge(playerAction);
        }

        return retValue;
    }

    @Override
    public AI clone() {
        // provides a copy of the AI with the same configuration (no need for full copy of internal state)
        return new BasicBot(this.TIME_BUDGET, this.ITERATIONS_BUDGET);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public String statisticsString() {
        // anything useful to be printed at the end of each game
        return "";
    }

    @Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
        // used for what it says on the tin
    }

}

