package com.chess.gui;

public class Main {

    static void main(final String[] args) {

        // Safely invoke the UI thread loop using Swing Utilities
        javax.swing.SwingUtilities.invokeLater(() -> Table.get().show());
    }
}