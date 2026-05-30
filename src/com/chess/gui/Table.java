package com.chess.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.MiniMax;
import com.chess.engine.player.ai.MoveStrategy;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table extends Observable {

    private final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;

    private Board chessBoard;

    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;

    private boolean highlightLegalMoves;

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(950, 800);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(650, 650);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);
    private static final String PIECE_IMAGE_PATH = "art/simple/";

    private final Color lightTileColor = Color.decode("#FFFFFF");
    private final Color darkTileColor = Color.decode("#222222");

    private static final Table INSTANCE = new Table();

    private Table() {
        this.gameFrame = new JFrame("Java Chess Engine");
        this.gameFrame.setLayout(new BorderLayout());
        final JMenuBar tableMenuBar = createTableMenuBar();
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.chessBoard = Board.createStandardBoard();
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.addObserver(new TableGameAIWatcher());
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = true;

        this.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);

        final JPanel centerWrapperPanel = new JPanel(new GridBagLayout());
        centerWrapperPanel.setBackground(Color.decode("#2f3542"));
        centerWrapperPanel.add(this.boardPanel);
        this.gameFrame.add(centerWrapperPanel, BorderLayout.CENTER);

        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.gameFrame.setLocationRelativeTo(null);
        this.gameFrame.setVisible(true);
    }

    public static Table get() {
        return INSTANCE;
    }

    public void show() {
        Table.get().getMoveLog().clear();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
    }

    public Board getGameBoard() {
        return this.chessBoard;
    }

    public MoveLog getMoveLog() {
        return this.moveLog;
    }

    public GameHistoryPanel getGameHistoryPanel() {
        return this.gameHistoryPanel;
    }

    public TakenPiecesPanel getTakenPiecesPanel() {
        return this.takenPiecesPanel;
    }

    public BoardPanel getBoardPanel() {
        return this.boardPanel;
    }

    public GameSetup getGameSetup() {
        return this.gameSetup;
    }

    private void updateGameBoard(final Board board) {
        this.chessBoard = board;
    }

    private void updateComputerMove(final Move move) {
        final MoveTransition transition = this.chessBoard.currentPlayer().makeMove(move);
        if (transition.getMoveStatus().isDone()) {
            this.chessBoard = transition.getTransitionBoard();
            this.moveLog.addMove(move);
            this.gameHistoryPanel.redo(chessBoard, this.moveLog);
            this.takenPiecesPanel.redo(this.moveLog);
            this.boardPanel.drawBoard(chessBoard);

            checkGameStatus();

            setChanged();
            notifyObservers(move);
        }
    }

    private JMenuBar createTableMenuBar() {
        final JMenuBar tableMenuBar = new JMenuBar();
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
        return tableMenuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem resetMenuItem = new JMenuItem("New Game");
        resetMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoAllMoves();
            }
        });
        fileMenu.add(resetMenuItem);

        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private JMenu createPreferencesMenu() {
        final JMenu preferencesMenu = new JMenu("Preferences");
        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip Board Orientation");
        flipBoardMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boardDirection = boardDirection.opposite();
                boardPanel.drawBoard(chessBoard);
            }
        });
        preferencesMenu.add(flipBoardMenuItem);

        preferencesMenu.addSeparator();
        final JCheckBoxMenuItem cbLegalMoveHighlighter = new JCheckBoxMenuItem("Highlight Legal Moves", true);
        cbLegalMoveHighlighter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                highlightLegalMoves = cbLegalMoveHighlighter.isSelected();
                boardPanel.drawBoard(chessBoard);
            }
        });
        preferencesMenu.add(cbLegalMoveHighlighter);
        return preferencesMenu;
    }

    private JMenu createOptionsMenu() {
        final JMenu optionsMenu = new JMenu("Options");
        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Match Variant");
        setupGameMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Table.get().getGameSetup().promptUser();
                Table.get().moveMadeUpdate(Table.get().getGameBoard());
            }
        });
        optionsMenu.add(setupGameMenuItem);
        return optionsMenu;
    }

    private void moveMadeUpdate(final Board board) {
        setChanged();
        notifyObservers(board);
    }

    private void undoAllMoves() {
        this.chessBoard = Board.createStandardBoard();
        this.moveLog.clear();
        this.gameHistoryPanel.redo(chessBoard, this.moveLog);
        this.takenPiecesPanel.redo(this.moveLog);
        this.boardPanel.drawBoard(chessBoard);
    }

    private void checkGameStatus() {
        boolean hasLegalMoves = false;
        if (this.chessBoard.currentPlayer().getLegalMoves() != null) {
            for (final Move move : this.chessBoard.currentPlayer().getLegalMoves()) {
                final MoveTransition transition = this.chessBoard.currentPlayer().makeMove(move);
                if (transition.getMoveStatus().isDone()) {
                    hasLegalMoves = true;
                    break;
                }
            }
        }

        if (!hasLegalMoves) {
            if (this.chessBoard.currentPlayer().isInCheck()) {
                String winningSide = this.chessBoard.currentPlayer().getAlliance().isWhite() ? "Black" : "White";
                JOptionPane.showMessageDialog(this.gameFrame,
                        "CHECKMATE! " + winningSide + " wins the game!",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this.gameFrame,
                        "STALEMATE! The match ends in a draw.",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public enum BoardDirection {
        NORMAL {
            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };
        abstract BoardDirection opposite();
    }

    private class BoardPanel extends JPanel {
        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8, 8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel tilePanel : boardDirection == BoardDirection.NORMAL ? boardTiles : reverseList(boardTiles)) {
                tilePanel.assignTileColor();
                tilePanel.assignPieceIcon(board);
                tilePanel.validateHighlighter(board);
                add(tilePanel);
            }
            validate();
            repaint();
        }
    }

    private static <T> List<T> reverseList(final List<T> list) {
        final List<T> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    public static class MoveLog {
        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return this.moves;
        }

        void addMove(final Move move) {
            this.moves.add(move);
        }

        void clear() {
            this.moves.clear();
        }
    }

    private class TilePanel extends JPanel {
        private final int tileId;

        TilePanel(final BoardPanel boardPanel, final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignPieceIcon(chessBoard);

            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (isRightMouseButton(e)) {
                        sourceTile = null;
                        destinationTile = null;
                        humanMovedPiece = null;
                    } else if (isLeftMouseButton(e)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getTile(tileId);
                            humanMovedPiece = sourceTile.getPiece();
                            if (humanMovedPiece == null || humanMovedPiece.getPieceAlliance() != chessBoard.currentPlayer().getAlliance()) {
                                sourceTile = null;
                                humanMovedPiece = null;
                            }
                        } else {
                            final Tile clickedTile = chessBoard.getTile(tileId);

                            if (clickedTile.isTileOccupied() && clickedTile.getPiece().getPieceAlliance() == chessBoard.currentPlayer().getAlliance()) {
                                sourceTile = clickedTile;
                                humanMovedPiece = clickedTile.getPiece();
                                destinationTile = null;
                            } else {
                                destinationTile = clickedTile;
                                final Move move = Move.MoveFactory.createMove(chessBoard, sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());
                                final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                                if (transition.getMoveStatus().isDone()) {
                                    chessBoard = transition.getTransitionBoard();
                                    moveLog.addMove(move);
                                    gameHistoryPanel.redo(chessBoard, moveLog);
                                    takenPiecesPanel.redo(moveLog);

                                    checkGameStatus();

                                    if (gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
                                        Table.get().moveMadeUpdate(chessBoard);
                                    }
                                }
                                sourceTile = null;
                                destinationTile = null;
                                humanMovedPiece = null;
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            boardPanel.drawBoard(chessBoard);
                        }
                    });
                }

                @Override public void mousePressed(MouseEvent e) {}
                @Override public void mouseReleased(MouseEvent e) {}
                @Override public void mouseEntered(MouseEvent e) {}
                @Override public void mouseExited(MouseEvent e) {}
            });
        }

        void assignTileColor() {
            boolean isLight = ((tileId / 8) + tileId) % 2 == 0;
            setBackground(isLight ? lightTileColor : darkTileColor);
        }

        void assignPieceIcon(final Board board) {
            this.removeAll();
            if (board.getTile(this.tileId).isTileOccupied()) {
                try {
                    final BufferedImage image = ImageIO.read(new File(PIECE_IMAGE_PATH +
                            board.getTile(this.tileId).getPiece().getPieceAlliance().toString().substring(0, 1) +
                            board.getTile(this.tileId).getPiece().toString() + ".gif"));
                    add(new JLabel(new ImageIcon(image)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void validateHighlighter(final Board board) {
            setBorder(null);

            if (highlightLegalMoves) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            add(new JLabel(new ImageIcon(ImageIO.read(new File("art/misc/green_dot.png")))));
                        } catch (Exception e) {
                            setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
                        }
                    }
                }
            }
            revalidate();
            repaint();
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if (humanMovedPiece != null && humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }
    }

    private static class TableGameAIWatcher implements Observer {
        @Override
        public void update(final Observable o, final Object arg) {
            if (Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer())) {
                final AIThinkingTask task = new AIThinkingTask();
                task.execute();
            }
        }
    }

    private static class AIThinkingTask extends SwingWorker<Move, Void> {
        private AIThinkingTask() {}

        @Override
        protected Move doInBackground() {
            final MoveStrategy minimax = new MiniMax(Table.get().getGameSetup().getSearchDepth());
            final MoveTransition transition = minimax.execute(Table.get().getGameBoard());
            return transition.getMove();
        }

        @Override
        protected void done() {
            try {
                final Move bestMove = get();
                if (bestMove != null) {
                    Table.get().updateComputerMove(bestMove);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}