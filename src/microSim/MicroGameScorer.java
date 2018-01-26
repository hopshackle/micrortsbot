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
        // we just determine who won, and set up the array.
        double[] retValue = new double[2];
        if (game instanceof MicroGame) {
            MicroGame microGame = (MicroGame) game;
            GameState gs = microGame.getGameState();
            if (gs.gameover()) {
                int winner = gs.winner();
                retValue[winner] = WIN_SCORE;
                retValue[1 - winner] = -WIN_SCORE;
            } else {
                for (int i = 0; i < 2; i++) {
                    retValue[i] = valueFunction.evaluate(i, 1 - i, gs) / 5.0;
                    if (retValue[i] > 0.80 * WIN_SCORE) retValue[i] = 0.8 * WIN_SCORE;
                    if (retValue[i] < -0.80 * WIN_SCORE) retValue[i] = -0.8 * WIN_SCORE;
                }
            }
        } else {
            throw new AssertionError("Cannot use MicroGameScorer on " + game.getClass());
        }
        return retValue;
    }

}
