package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import java.util.Collection;


public abstract class Piece {
    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    protected final boolean isfirstMove;

  public  Piece(final int piecePosition, final Alliance pieceAlliance){
        this.pieceAlliance = pieceAlliance;
        this.piecePosition = piecePosition;
        this.isfirstMove =false;
    }
    public int getPiecePosition(){
        return this.piecePosition;
    }
    public Alliance getpieceAlliance(){
        return this.pieceAlliance;
    }
    public boolean isfirstMove(){
        return this.isfirstMove;
    }

    public abstract Collection<Move> calculateLegalMoves(final Board board);

      public enum PieceType{
          PAWN("p"),
          KNIGHT("N"),
          BISHOP("B"),
          ROOK("R"),
          QUEEN("Q"),
          KING("K");

           final private String pieceName;

          PieceType(final String pieceName){
              this.pieceName=pieceName;
          }
          @Override
          public String toString(){
              return this.pieceName;
          }
      }
}
