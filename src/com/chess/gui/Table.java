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
import javax.swing.border.BevelBorder;
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
    private final GameTimerPanel gameTimerPanel;

    private Board chessBoard;

    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;

    private boolean highlightLegalMoves;

    // Fix: Global state flag to track if the game is finalized
    private boolean isGameOver;

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(1000, 850);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(650, 650);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);
    private static final String PIECE_IMAGE_PATH = "art/simple/";

    private final Color lightTileColor = Color.decode("#F0D9B5");
    private final Color darkTileColor = Color.decode("#B58863");

    private int consecutiveNonPawnNonCaptureMoves = 0;
    private final Map<String, Integer> boardHistoryTracker = new HashMap<>();

    private static final Table INSTANCE = new Table();

    private Table() {
        this.gameFrame = new JFrame("Java Chess Engine - Ultimate Edition");
        this.gameFrame.setLayout(new BorderLayout());
        final JMenuBar tableMenuBar = createTableMenuBar();
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.chessBoard = Board.createStandardBoard();
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.gameTimerPanel = new GameTimerPanel();
        this.addObserver(new TableGameAIWatcher());
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = true;
        this.isGameOver = false; // Fresh start

        this.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);

        final JPanel centerWrapperPanel = new JPanel(new GridBagLayout());
        centerWrapperPanel.setBackground(Color.decode("#2f3542"));
        centerWrapperPanel.add(this.boardPanel);
        this.gameFrame.add(centerWrapperPanel, BorderLayout.CENTER);

        final JPanel rightColumnPanel = new JPanel(new BorderLayout());
        rightColumnPanel.add(this.gameTimerPanel, BorderLayout.NORTH);
        rightColumnPanel.add(this.gameHistoryPanel, BorderLayout.CENTER);
        this.gameFrame.add(rightColumnPanel, BorderLayout.EAST);

        recordBoardStateSignature(this.chessBoard);

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
        Table.get().getGameTimerPanel().resetTimers();
        clearRuleTrackers();
        this.isGameOver = false;
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

    public GameTimerPanel getGameTimerPanel() {
        return this.gameTimerPanel;
    }

    private void updateGameBoard(final Board board) {
        this.chessBoard = board;
    }

    private void updateComputerMove(final Move move) {
        // Fix: Block AI execution if game state is over
        if (this.isGameOver) return;

        final MoveTransition transition = this.chessBoard.currentPlayer().makeMove(move);
        if (transition.getMoveStatus().isDone()) {
            this.chessBoard = transition.getTransitionBoard();
            this.moveLog.addMove(move);

            trackRuleVariables(move);
            recordBoardStateSignature(this.chessBoard);

            this.gameHistoryPanel.redo(chessBoard, this.moveLog);
            this.takenPiecesPanel.redo(this.moveLog);
            this.boardPanel.drawBoard(chessBoard);

            this.gameTimerPanel.switchTurn(this.chessBoard.currentPlayer().getAlliance().isWhite());
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

        optionsMenu.addSeparator();

        final JMenuItem offerDrawItem = new JMenuItem("Offer Mutual Draw (Tie)");
        offerDrawItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isGameOver) {
                    JOptionPane.showMessageDialog(gameFrame, "The match is already over!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int response = JOptionPane.showConfirmDialog(gameFrame, "Opponent offers a mutual draw tie. Accept?", "Draw Offer", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    isGameOver = true;
                    gameTimerPanel.stopTimers();
                    JOptionPane.showMessageDialog(gameFrame, "Match ends in a Draw by Mutual Agreement!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        optionsMenu.add(offerDrawItem);

        return optionsMenu;
    }

    private void moveMadeUpdate(final Board board) {
        setChanged();
        notifyObservers(board);
    }

    private void undoAllMoves() {
        this.chessBoard = Board.createStandardBoard();
        this.moveLog.clear();
        clearRuleTrackers();
        this.isGameOver = false; // Reset game state flag
        recordBoardStateSignature(this.chessBoard);
        this.gameHistoryPanel.redo(chessBoard, this.moveLog);
        this.takenPiecesPanel.redo(this.moveLog);
        this.boardPanel.drawBoard(chessBoard);
        this.gameTimerPanel.resetTimers();
    }

    private void clearRuleTrackers() {
        this.consecutiveNonPawnNonCaptureMoves = 0;
        this.boardHistoryTracker.clear();
    }

    private void trackRuleVariables(final Move move) {
        if (move.isAttack() || move.getMovedPiece().getPieceType().toString().equalsIgnoreCase("P")) {
            this.consecutiveNonPawnNonCaptureMoves = 0;
        } else {
            this.consecutiveNonPawnNonCaptureMoves++;
        }
    }

    private void recordBoardStateSignature(final Board board) {
        String stateSignature = generateBoardStringSignature(board);
        this.boardHistoryTracker.put(stateSignature, this.boardHistoryTracker.getOrDefault(stateSignature, 0) + 1);
    }

    private String generateBoardStringSignature(final Board board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            Tile tile = board.getTile(i);
            if (tile.isTileOccupied()) {
                sb.append(tile.getPiece().getPieceAlliance().toString().charAt(0))
                        .append(tile.getPiece().getPieceType().toString());
            } else {
                sb.append("-");
            }
        }
        sb.append(board.currentPlayer().getAlliance().toString());
        return sb.toString();
    }

    private void checkGameStatus() {
        if (this.isGameOver) return; // Safeguard wrapper

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

        // Rule 1: Checkmate / Stalemate Check
        if (!hasLegalMoves) {
            this.isGameOver = true;
            this.gameTimerPanel.stopTimers();
            if (this.chessBoard.currentPlayer().isInCheck()) {
                String winningSide = this.chessBoard.currentPlayer().getAlliance().isWhite() ? "Black" : "White";
                JOptionPane.showMessageDialog(this.gameFrame, "CHECKMATE! " + winningSide + " wins the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this.gameFrame, "STALEMATE! The match ends in a draw tie.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        // Rule 2: 50-Move Rule
        if (this.consecutiveNonPawnNonCaptureMoves >= 100) {
            this.isGameOver = true;
            this.gameTimerPanel.stopTimers();
            JOptionPane.showMessageDialog(this.gameFrame, "TIE DRAW! 50 consecutive moves completed without captures or pawn pushes.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Rule 3: Insufficient Force Material Check
        if (hasInsufficientMaterial(this.chessBoard)) {
            this.isGameOver = true;
            this.gameTimerPanel.stopTimers();
            JOptionPane.showMessageDialog(this.gameFrame, "TIE DRAW! Insufficient material to force checkmate on the board.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Rule 4: Threefold Repetition Check
        String currentSignature = generateBoardStringSignature(this.chessBoard);
        if (this.boardHistoryTracker.getOrDefault(currentSignature, 0) >= 3) {
            this.isGameOver = true;
            this.gameTimerPanel.stopTimers();
            JOptionPane.showMessageDialog(this.gameFrame, "TIE DRAW! The exact same board position has occurred 3 times (Threefold Repetition).", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean hasInsufficientMaterial(final Board board) {
        List<Piece> activePieces = new ArrayList<>();
        activePieces.addAll(board.getWhitePieces());
        activePieces.addAll(board.getBlackPieces());

        if (activePieces.size() == 2) {
            return true;
        }

        if (activePieces.size() == 3) {
            for (final Piece p : activePieces) {
                String type = p.getPieceType().toString();
                if (type.equalsIgnoreCase("B") || type.equalsIgnoreCase("N")) {
                    return true;
                }
            }
        }

        if (activePieces.size() == 4) {
            Piece whiteBishop = null;
            Piece blackBishop = null;
            for (Piece p : activePieces) {
                if (p.getPieceType().toString().equalsIgnoreCase("B")) {
                    if (p.getPieceAlliance().isWhite()) whiteBishop = p;
                    else blackBishop = p;
                }
            }
            if (whiteBishop != null && blackBishop != null) {
                int whiteCoord = whiteBishop.getPiecePosition();
                int blackCoord = blackBishop.getPiecePosition();
                boolean whiteIsLightSquare = ((whiteCoord / 8) + whiteCoord) % 2 == 0;
                boolean blackIsLightSquare = ((blackCoord / 8) + blackCoord) % 2 == 0;
                return whiteIsLightSquare == blackIsLightSquare;
            }
        }
        return false;
    }

    public enum BoardDirection {
        NORMAL { @Override BoardDirection opposite() { return FLIPPED; } },
        FLIPPED { @Override BoardDirection opposite() { return NORMAL; } };
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
        MoveLog() { this.moves = new ArrayList<>(); }
        public List<Move> getMoves() { return this.moves; }
        void addMove(final Move move) { this.moves.add(move); }
        void clear() { this.moves.clear(); }
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
                    // Fix: If game is over, instantly ignore click input
                    if (isGameOver) return;

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

                                    trackRuleVariables(move);
                                    recordBoardStateSignature(chessBoard);

                                    gameHistoryPanel.redo(chessBoard, moveLog);
                                    takenPiecesPanel.redo(moveLog);

                                    if(!gameTimerPanel.isClockRunning()) {
                                        gameTimerPanel.startClockSystem();
                                    }

                                    gameTimerPanel.switchTurn(chessBoard.currentPlayer().getAlliance().isWhite());
                                    checkGameStatus();

                                    if (!isGameOver && gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
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
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, getBackground().brighter(), getBackground().darker()));
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
            // Fix: Clean out green target highlights if game state is finished
            if (highlightLegalMoves && !isGameOver) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            add(new JLabel(new ImageIcon(ImageIO.read(new File("art/misc/green_dot.png")))));
                        } catch (Exception e) {
                            setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
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
            if (!Table.get().isGameOver && Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer())) {
                final AIThinkingTask task = new AIThinkingTask();
                task.execute();
            }
        }
    }

    private static class AIThinkingTask extends SwingWorker<Move, Void> {
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

    public class GameTimerPanel extends JPanel {
        private final JLabel whiteTimerLabel;
        private final JLabel blackTimerLabel;
        private final JComboBox<String> timeChooser;
        private final javax.swing.Timer countdownTimer;

        private int whiteTimeLeftSec;
        private int blackTimeLeftSec;
        private boolean isWhiteTurn;
        private boolean clockRunning;

        GameTimerPanel() {
            super(new BorderLayout());
            this.setBackground(Color.decode("#2f3542"));
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Match Clock",
                    0, 0, new Font("Arial", Font.BOLD, 12), Color.WHITE));

            this.whiteTimeLeftSec = 600;
            this.blackTimeLeftSec = 600;
            this.isWhiteTurn = true;
            this.clockRunning = false;

            this.whiteTimerLabel = new JLabel("White: 10:00", SwingConstants.CENTER);
            this.blackTimerLabel = new JLabel("Black: 10:00", SwingConstants.CENTER);
            Font clockFont = new Font("Monospaced", Font.BOLD, 18);
            this.whiteTimerLabel.setFont(clockFont);
            this.blackTimerLabel.setFont(clockFont);
            this.whiteTimerLabel.setForeground(Color.GREEN);
            this.blackTimerLabel.setForeground(Color.WHITE);

            String[] timeOptions = {"10 Minutes", "5 Minutes", "1 Minute"};
            this.timeChooser = new JComboBox<>(timeOptions);
            this.timeChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!clockRunning) {
                        resetTimers();
                    }
                }
            });

            final JPanel labelsGrid = new JPanel(new GridLayout(1, 2));
            labelsGrid.setBackground(Color.decode("#1e222b"));
            labelsGrid.add(whiteTimerLabel);
            labelsGrid.add(blackTimerLabel);

            this.add(timeChooser, BorderLayout.NORTH);
            this.add(labelsGrid, BorderLayout.CENTER);

            this.countdownTimer = new javax.swing.Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isGameOver) {
                        countdownTimer.stop();
                        return;
                    }
                    if (isWhiteTurn) {
                        whiteTimeLeftSec--;
                        updateLabelText(whiteTimerLabel, "White: ", whiteTimeLeftSec);
                        if (whiteTimeLeftSec <= 0) {
                            handleTimeout("Black Wins! White ran out of time.");
                        }
                    } else {
                        blackTimeLeftSec--;
                        updateLabelText(blackTimerLabel, "Black: ", blackTimeLeftSec);
                        if (blackTimeLeftSec <= 0) {
                            handleTimeout("White Wins! Black ran out of time.");
                        }
                    }
                }
            });
        }

        private void updateLabelText(JLabel label, String prefix, int totalSeconds) {
            int mins = totalSeconds / 60;
            int secs = totalSeconds % 60;
            label.setText(String.format("%s%02d:%02d", prefix, mins, secs));
            if (totalSeconds <= 30) {
                label.setForeground(Color.RED);
            }
        }

        public void startClockSystem() {
            this.clockRunning = true;
            this.timeChooser.setEnabled(false);
            this.countdownTimer.start();
        }

        public void switchTurn(boolean isWhiteNow) {
            this.isWhiteTurn = isWhiteNow;
            if (isWhiteTurn) {
                whiteTimerLabel.setForeground(Color.GREEN);
                blackTimerLabel.setForeground(Color.WHITE);
            } else {
                whiteTimerLabel.setForeground(Color.WHITE);
                blackTimerLabel.setForeground(Color.GREEN);
            }
        }

        public void stopTimers() {
            this.countdownTimer.stop();
            this.clockRunning = false;
            this.timeChooser.setEnabled(true);
        }

        public void resetTimers() {
            stopTimers();
            int selectedIndex = timeChooser.getSelectedIndex();
            int baseSeconds = selectedIndex == 0 ? 600 : selectedIndex == 1 ? 300 : 60;

            this.whiteTimeLeftSec = baseSeconds;
            this.blackTimeLeftSec = baseSeconds;
            this.isWhiteTurn = true;

            updateLabelText(whiteTimerLabel, "White: ", whiteTimeLeftSec);
            updateLabelText(blackTimerLabel, "Black: ", blackTimeLeftSec);
            this.whiteTimerLabel.setForeground(Color.GREEN);
            this.blackTimerLabel.setForeground(Color.WHITE);
        }

        public boolean isClockRunning() { return this.clockRunning; }

        private void handleTimeout(String message) {
            isGameOver = true;
            stopTimers();
            JOptionPane.showMessageDialog(gameFrame, message, "Match Timeout", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}