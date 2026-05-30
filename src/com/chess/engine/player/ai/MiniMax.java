package com.chess.engine.player.ai;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.MoveStatus;

public class MiniMax implements MoveStrategy {

    private final BoardEvaluator boardEvaluator;
    private final int searchDepth;

    public MiniMax(final int searchDepth) {
        this.boardEvaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
    }

    @Override
    public String toString() {
        return "MiniMax";
    }

    @Override
    public MoveTransition execute(Board board) {
        final long startTime = System.currentTimeMillis();
        Move bestMove = null;

        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;

        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);

        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.moveStatus().isDone()) {
                currentValue = board.currentPlayer().getAlliance() == Alliance.WHITE ?
                        min(moveTransition.transitionBoard(), this.searchDepth - 1) :
                        max(moveTransition.transitionBoard(), this.searchDepth - 1);

                if (board.currentPlayer().getAlliance() == Alliance.WHITE && currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                } else if (board.currentPlayer().getAlliance() == Alliance.BLACK && currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                }
            }
        }

        final long executionTime = System.currentTimeMillis() - startTime;
        System.out.println("Execution time: " + executionTime + "ms");

        // Execute the calculated best move inside a proper MoveTransition wrapper to satisfy the interface return type
        if (bestMove != null) {
            return board.currentPlayer().makeMove(bestMove);
        }

        return new MoveTransition(board, null, MoveStatus.ILLEGAL_MOVE);
    }

    public int min(final Board board, final int depth) {
        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }
        int lowestSeenValue = Integer.MAX_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.moveStatus().isDone()) {
                final int currentValue = max(moveTransition.transitionBoard(), depth - 1);
                if (currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                }
            }
        }
        return lowestSeenValue;
    }

    public int max(final Board board, final int depth) {
        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }
        int highestSeenValue = Integer.MIN_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.moveStatus().isDone()) {
                final int currentValue = min(moveTransition.transitionBoard(), depth - 1);
                if (currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                }
            }
        }
        return highestSeenValue;
    }

    private static boolean isEndGameScenario(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }
}