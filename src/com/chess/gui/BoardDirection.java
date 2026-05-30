package com.chess.gui;

public enum BoardDirection {
    NORMAL {
        @Override
        public BoardDirection opposite() {
            return FLIPPED;
        }

        @Override
        public int traverse(final int coordinate) {
            return coordinate;
        }
    },
    FLIPPED {
        @Override
        public BoardDirection opposite() {
            return NORMAL;
        }

        @Override
        public int traverse(final int coordinate) {
            // Flips the tile coordinate perspective (63 - coordinate) for GUI rendering
            return 63 - coordinate;
        }
    };

    public abstract BoardDirection opposite();
    public abstract int traverse(final int coordinate);
}