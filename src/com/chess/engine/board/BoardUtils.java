package com.chess.engine.board;

import java.util.HashMap;
import java.util.Map;

public class BoardUtils {

    public static final boolean[] FIRST_COLUMN = initColumn(0);
    public static final boolean[] SECOND_COLUMN = initColumn(1);
    public static final boolean[] SEVENTH_COLUMN = initColumn(6);
    public static final boolean[] EIGHTH_COLUMN = initColumn(7);

    public static final boolean[] EIGHTH_RANK = initRank(0);
    public static final boolean[] SEVENTH_RANK = initRank(8);
    public static final boolean[] SIXTH_RANK = initRank(16);
    public static final boolean[] FIFTH_RANK = initRank(24);
    public static final boolean[] FOURTH_RANK = initRank(32);
    public static final boolean[] THIRD_RANK = initRank(40);
    public static final boolean[] SECOND_RANK = initRank(48);
    public static final boolean[] FIRST_RANK = initRank(56);

    public static final int NUM_TILES = 64;
    public static final int NUM_TILES_PER_ROW = 8;

    private BoardUtils() {
        throw new RuntimeException("You cannot instantiate BoardUtils!");
    }

    private static boolean[] initColumn(int columnNumber) {
        final boolean[] column = new boolean[NUM_TILES];
        do {
            column[columnNumber] = true;
            columnNumber += NUM_TILES_PER_ROW;
        } while(columnNumber < NUM_TILES);
        return column;
    }

    private static boolean[] initRank(int rowNumber) {
        final boolean[] rank = new boolean[NUM_TILES];
        do {
            rank[rowNumber] = true;
            rowNumber++;
        } while(rowNumber % NUM_TILES_PER_ROW != 0);
        return rank;
    }

    public static boolean isValidTileCoordinate(final int coordinate) {
        return coordinate >= 0 && coordinate < NUM_TILES;
    }

    // This is the missing method that Move.java was looking for!
    public static String getPositionAtCoordinate(final int coordinate) {
        final String[] ALPHANUMERIC_BOARD = {
                "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
                "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
                "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
                "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
                "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
                "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
                "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
                "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"
        };
        return ALPHANUMERIC_BOARD[coordinate];
    }
}