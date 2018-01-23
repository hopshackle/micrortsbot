package microSim;

import ai.evaluation.SimpleEvaluationFunction;
import hopshackle.simulation.*;
import rts.*;

import java.util.*;

import ai.evaluation.*;

/**
 * Created by James on 20/01/2018.
 */
public class MicroGameScorer implements GameScoreCalculator {

    private EvaluationFunction valueFunction = new SimpleEvaluationFunction();

    private double WIN_SCORE = 100;

    @Override
    public double[] finalScores(Game game) {
        // We need to get the score for each player, and put this into the array
        // at the position for all MicroAgents with that playerID
        List<MicroAgent> allAgents = game.getAllPlayers();
        double[] retValue = new double[allAgents.size()];
        if (game instanceof MicroGame) {
            MicroGame microGame = (MicroGame) game;
            GameState gs = microGame.getGameState();
            if (gs.gameover()) {
                int winner = gs.winner();
                for (int i = 0; i < retValue.length; i++) {
                    int p = allAgents.get(i).getUnit().getPlayer();
                    if (p == winner) {
                        retValue[i] = WIN_SCORE;
                    } else {
                        retValue[i] = -WIN_SCORE;
                    }
                }
            } else {
                int winner = gs.winner();
                for (int i = 0; i < retValue.length; i++) {
                    int p = allAgents.get(i).getUnit().getPlayer();
                    int opponent = (p == 0) ? 1 : 0;
                    retValue[i] = valueFunction.evaluate(p, opponent, gs);
                }
            }
        } else {
            throw new AssertionError("Cannot use MicroGameScorer on " + game.getClass());
        }
        return retValue;
    }

}
