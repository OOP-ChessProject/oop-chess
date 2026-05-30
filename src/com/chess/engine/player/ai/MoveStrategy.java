package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.player.MoveTransition;

public interface MoveStrategy {

    MoveTransition execute(Board board);

}