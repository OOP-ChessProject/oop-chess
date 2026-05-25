package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Boardutils;

import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Knight extends Piece {

    private final static int[] CANDIDATE_MOVE_COORDINATES = {
            -17, -15, -10, -6, 6, 10, 15, 17
    };

    public Knight(final Alliance PieceAlliance, final int piecePosition) {

        super(piecePosition, PieceAlliance);
    }

    @Override
    public int getPieceValue() {
        return 0;
    }

    @Override
    public Collection<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for(final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATES) {

            final int candidateDestinationCoordinate =
                    this.piecePosition + currentCandidateOffset;

            if(Boardutils.isValidTileCoordinate(candidateDestinationCoordinate)) {

                if(isFirstColumnExclusion(this.piecePosition,
                        currentCandidateOffset) ||
                        isSecondColumnExclusion(this.piecePosition,
                                currentCandidateOffset) ||
                        isSeventhColumnExclusion(this.piecePosition,
                                currentCandidateOffset) ||
                        isEighthColumnExclusion(this.piecePosition,
                                currentCandidateOffset)) {

                    continue;
                }

                final Tile candidateDestinationTile =
                        board.getTile(candidateDestinationCoordinate);

                if(!candidateDestinationTile.isTileOccupied()) {

                    legalMoves.add(new Move.MajorMove(board,this,candidateDestinationCoordinate));

                } else {

                    final Piece pieceAtDestination =
                            candidateDestinationTile.getPiece();

                    final Alliance pieceAlliance =
                            pieceAtDestination.getpieceAlliance();

                    if(this.pieceAlliance != pieceAlliance) {
                        legalMoves.add(new Move.AttackMove(board,this,candidateDestinationCoordinate,pieceAtDestination));
                    }
                }
            }
        }

        return Collections.unmodifiableList(legalMoves);
    }

    @Override
    public Piece movePiece(Move move) {
        return null;
    }

    @Override
    public String toString(){
        return PieceType.KNIGHT.toString();
    }


    private static boolean isFirstColumnExclusion(final int currentPosition,
                                                  final int candidateOffset) {

        return Boardutils.FIRST_COLUMN[currentPosition] &&
                (candidateOffset == -17 ||
                        candidateOffset == -10 ||
                        candidateOffset == 6 ||
                        candidateOffset == 15);
    }

    private static boolean isSecondColumnExclusion(final int currentPosition,
                                                   final int candidateOffset) {

        return Boardutils.SECOND_COLUMN[currentPosition] &&
                (candidateOffset == -10 ||
                        candidateOffset == 6);
    }

    private static boolean isSeventhColumnExclusion(final int currentPosition,
                                                    final int candidateOffset) {

        return Boardutils.SEVENTH_COLUMN[currentPosition] &&
                (candidateOffset == -6 ||
                        candidateOffset == 10);
    }

    private static boolean isEighthColumnExclusion(final int currentPosition,
                                                   final int candidateOffset) {

        return Boardutils.EIGHTH_COLUMN[currentPosition] &&
                (candidateOffset == -15 ||
                        candidateOffset == -6 ||
                        candidateOffset == 10 ||
                        candidateOffset == 17);
    }

    }