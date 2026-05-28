package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class King extends Piece {
    private final static int[] CANDIDATE_MOVE_COORDINATE = {-9, -8, -7, -1, 1, 7, 8, 9};


    public King(Alliance pieceAlliance, int piecePositon) {
        super(piecePositon, pieceAlliance);
    }

    @Override
    public Collection<Move> calculateLegalMoves(Board board) {

        final List<Move> LegalMoves = new ArrayList<>();
        int candidiateDestinationCoordiante;

        for (final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATE) {
            final int candidateDestinationCoordinate = this.piecePosition + currentCandidateOffset;
            if (isFirstColumnExclusion(this.piecePosition,currentCandidateOffset)|| isEighthColumnExclusion(this.piecePosition,currentCandidateOffset)) {
                continue;
            }
            if (BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)) {
                final Tile candidateDestinationTile = board.getTile(candidateDestinationCoordinate);


                if (!candidateDestinationTile.isTileOccupied()) {

                    LegalMoves.add(new Move.MajorMove(board, this, candidateDestinationCoordinate));

                } else {

                    final Piece pieceAtDestination =
                            candidateDestinationTile.getPiece();

                    final Alliance pieceAlliance =
                            pieceAtDestination.getpieceAlliance();

                    if (this.pieceAlliance != pieceAlliance) {
                        LegalMoves.add(new Move.AttackMove(board, this, candidateDestinationCoordinate, pieceAtDestination));
                    }
                }
            }

        }
        return ImmutableList.copyOf(LegalMoves);
    }
    @Override
    public String toString(){
        return PieceType.KING.toString();
    }
    private static boolean isFirstColumnExclusion(final int currentPosition,
                                                  final int candidateOffset) {

        return BoardUtils.FIRST_COLUMN[currentPosition] &&
                (candidateOffset == -9||
                        candidateOffset == -1 ||
                        candidateOffset == 7);
    }

    private static boolean isEighthColumnExclusion(final int currentPosition,
                                                   final int candidateOffset) {

        return BoardUtils.EIGHTH_COLUMN[currentPosition] &&
                (candidateOffset == -7 ||
                        candidateOffset == 1||candidateOffset==9);
    }

}
