package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;

public final class StandardBoardEvaluator implements BoardEvaluator {

    // Relative weights for positional scoring parameters
    private static final int MOBILITY_WEIGHT = 5;
    private static final int CHECK_BONUS = 50;
    private static final int CHECKMATE_BONUS = 10000;

    @Override
    public int evaluate(final Board board, final int depth) {
        return scorePlayer(board, board.whitePlayer(), depth) -
                scorePlayer(board, board.blackPlayer(), depth);
    }

    private int scorePlayer(final Board board, final Player player, final int depth) {
        return pieceValue(player) +
                mobility(player) +
                check(player) +
                checkmate(player, depth);
    }

    // Accumulates total value points of all surviving active pieces for a player
    private static int pieceValue(final Player player) {
        int pieceValueScore = 0;
        for (final Piece piece : player.getActivePieces()) {
            pieceValueScore += piece.getPieceValue();
        }
        return pieceValueScore;
    }

    // Weights freedom of movement (number of options creates better position control)
    private static int mobility(final Player player) {
        return player.getLegalMoves().size() * MOBILITY_WEIGHT;
    }

    // Punishes configurations where a player is trapped in check
    private static int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    // Highest priority score multiplier factoring search depth (finding mate faster is preferred)
    private static int checkmate(final Player player, final int depth) {
        return player.getOpponent().isInCheckMate() ? CHECKMATE_BONUS * depthBonus(depth) : 0;
    }

    private static int depthBonus(final int depth) {
        return depth == 0 ? 1 : 100 * depth;
    }
}