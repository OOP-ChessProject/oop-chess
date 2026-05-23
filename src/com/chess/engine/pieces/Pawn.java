package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Boardutils;
import com.chess.engine.board.Move;
import com.google.common.collect.ImmutableList;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.chess.engine.board.Move.*;

public class Pawn extends Piece {

    private final static int[] CANDIDATE_MOVE_COORDINATE = {8, 16, 7, 9};

    public Pawn(final Alliance pieceAlliance, final int piecePosition) {
        super(piecePosition, pieceAlliance);
    }

    @Override
    public Collection<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for(final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATE) {

            final int candidateDestinationCoordinate =
                    this.piecePosition +
                            (this.pieceAlliance.getDirection() * currentCandidateOffset);

            if(!Boardutils.isValidTileCoordinate(candidateDestinationCoordinate)) {
                continue;
            }

            if(currentCandidateOffset == 8 &&
                    !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                legalMoves.add(new MajorMove(
                        board,
                        this,
                        candidateDestinationCoordinate));

            } else if(currentCandidateOffset == 16 &&
                    this.isfirstMove() &&
                    ((Boardutils.SECOND_ROW[this.piecePosition] &&
                            this.pieceAlliance.isBlack()) ||
                            (Boardutils.SEVENTH_ROW[this.piecePosition] &&
                                    this.pieceAlliance.isWhite()))) {

                final int behindCandidateDestinationCoordinate =
                        this.piecePosition +
                                (this.pieceAlliance.getDirection() * 8);

                if(!board.getTile(behindCandidateDestinationCoordinate).isTileOccupied() &&
                        !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                    legalMoves.add(new MajorMove(
                            board,
                            this,
                            candidateDestinationCoordinate));
                }

            } else if(currentCandidateOffset == 7 &&
                    !((Boardutils.EIGHTH_COLUMN[this.piecePosition] &&
                            this.pieceAlliance.isWhite()) ||
                            (Boardutils.FIRST_COLUMN[this.piecePosition] &&
                                    this.pieceAlliance.isBlack()))) {

                if(board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                    final Piece pieceOnCandidate =
                            board.getTile(candidateDestinationCoordinate).getPiece();

                    if(this.pieceAlliance !=
                            pieceOnCandidate.getpieceAlliance()) {

                        legalMoves.add(new MajorMove(
                                board,
                                this,
                                candidateDestinationCoordinate));
                    }
                }

            } else if(currentCandidateOffset == 9 &&
                    !((Boardutils.FIRST_COLUMN[this.piecePosition] &&
                            this.pieceAlliance.isWhite()) ||
                            (Boardutils.EIGHTH_COLUMN[this.piecePosition] &&
                                    this.pieceAlliance.isBlack()))) {

                if(board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                    final Piece pieceOnCandidate =
                            board.getTile(candidateDestinationCoordinate).getPiece();

                    if(this.pieceAlliance !=
                            pieceOnCandidate.getpieceAlliance()) {
                        legalMoves.add(new MajorMove(
                                board,
                                this,
                                candidateDestinationCoordinate));
                    }
                }
            }
        }

        return ImmutableList.copyOf(legalMoves);
    }
    @Override
    public String toString(){
        return PieceType.PAWN.toString();
    }


}
