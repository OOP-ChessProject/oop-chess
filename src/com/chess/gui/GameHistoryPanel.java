package com.chess.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.gui.Table.MoveLog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryPanel extends JPanel {

    private final DataModel model;
    private final JScrollPane scrollPane;
    private static final Dimension HISTORY_PANEL_DIMENSION = new Dimension(100, 400);

    public GameHistoryPanel() {
        this.setLayout(new BorderLayout());
        this.model = new DataModel();
        final JTable table = new JTable(this.model);
        table.setRowHeight(15);
        this.scrollPane = new JScrollPane(table);
        this.scrollPane.setColumnHeaderView(table.getTableHeader());
        this.scrollPane.setPreferredSize(HISTORY_PANEL_DIMENSION);
        this.add(this.scrollPane, BorderLayout.CENTER);
        this.setVisible(true);
    }

    /**
     * Redraws the history panel based on the current state of the board and move log.
     */
    public void redo(final Board board, final MoveLog moveLog) {
        int currentRow = 0;
        this.model.setRowCount(0);

        final List<String> moveHistory = new ArrayList<>();
        for (final Move move : moveLog.getMoves()) {
            moveHistory.add(move.toString());
        }

        // Bundle white and black moves sequentially into rows
        for (int i = 0; i < moveHistory.size(); i += 2) {
            final String whiteMove = moveHistory.get(i);
            final String blackMove = (i + 1 < moveHistory.size()) ? moveHistory.get(i + 1) : "";
            this.model.setValueAt(whiteMove, currentRow, 0);
            this.model.setValueAt(blackMove, currentRow, 1);
            currentRow++;
        }

        // Automatically snap the scrollbar to the bottom row on update
        final JScrollBar vertical = this.scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    /**
     * Custom lightweight TableModel implementation matching the video engine specifications.
     */
    private static class DataModel extends DefaultTableModel {

        private final List<Row> values;
        private static final String[] NAMES = {"White", "Black"};

        DataModel() {
            this.values = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return this.values == null ? 0 : this.values.size();
        }

        @Override
        public int getColumnCount() {
            return NAMES.length;
        }

        @Override
        public Object getValueAt(final int row, final int column) {
            final Row currentRow = this.values.get(row);
            if (column == 0) {
                return currentRow.getWhiteMove();
            } else if (column == 1) {
                return currentRow.getBlackMove();
            }
            return null;
        }

        @Override
        public void setValueAt(final Object aValue, final int row, final int column) {
            final Row currentRow;
            if (this.values.size() <= row) {
                currentRow = new Row();
                this.values.add(currentRow);
            } else {
                currentRow = this.values.get(row);
            }

            if (column == 0) {
                currentRow.setWhiteMove((String) aValue);
                this.fireTableRowsInserted(row, row);
            } else if (column == 1) {
                currentRow.setBlackMove((String) aValue);
                this.fireTableCellUpdated(row, column);
            }
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return String.class;
        }

        @Override
        public String getColumnName(final int column) {
            return NAMES[column];
        }

        @Override
        public void setRowCount(final int rowCount) {
            if (rowCount == 0) {
                this.values.clear();
            }
            super.setRowCount(rowCount);
        }
    }

    /**
     * Internal container representing a full round of moves (White and Black responses).
     */
    private static class Row {

        private String whiteMove;
        private String blackMove;

        Row() {}

        public String getWhiteMove() {
            return this.whiteMove;
        }

        public void setWhiteMove(final String move) {
            this.whiteMove = move;
        }

        public String getBlackMove() {
            return this.blackMove;
        }

        public void setBlackMove(final String move) {
            this.blackMove = move;
        }
    }
}