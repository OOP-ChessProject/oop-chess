package com.chess.gui;

import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.chess.gui.Table.MoveLog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TakenPiecesPanel extends JPanel {

    private final JPanel northPanel;
    private final JPanel southPanel;

    private static final Color PANEL_BACKGROUND_COLOR = Color.decode("#FDFDFD");
    private static final Dimension TAKEN_PIECES_DIMENSION = new Dimension(40, 80);
    private static final EtchedBorder PANEL_BORDER = new EtchedBorder(EtchedBorder.RAISED);

    public TakenPiecesPanel() {
        super(new BorderLayout());
        this.setBackground(PANEL_BACKGROUND_COLOR);
        this.setBorder(PANEL_BORDER);

        // Two separate sub-panels to isolate white casualties from black casualties
        this.northPanel = new JPanel(new GridLayout(8, 2));
        this.southPanel = new JPanel(new GridLayout(8, 2));
        this.northPanel.setBackground(PANEL_BACKGROUND_COLOR);
        this.southPanel.setBackground(PANEL_BACKGROUND_COLOR);

        this.add(this.northPanel, BorderLayout.NORTH);
        this.add(this.southPanel, BorderLayout.SOUTH);
        this.setPreferredSize(TAKEN_PIECES_DIMENSION);
    }

    /**
     * Re-evaluates the move log history, extracts all captured pieces, and repaints the panel rows.
     */
    public void redo(final MoveLog moveLog) {
        this.northPanel.removeAll();
        this.southPanel.removeAll();

        final List<Piece> whiteTakenPieces = new ArrayList<>();
        final List<Piece> blackTakenPieces = new ArrayList<>();

        // Extract any piece that was consumed during an active board transition
        for (final Move move : moveLog.getMoves()) {
            if (move.isAttack()) {
                final Piece attackedPiece = move.getAttackedPiece();
                if (attackedPiece.getPieceAlliance().isWhite()) {
                    whiteTakenPieces.add(attackedPiece);
                } else if (attackedPiece.getPieceAlliance().isBlack()) {
                    blackTakenPieces.add(attackedPiece);
                }
            }
        }

        // Sort both collections by piece value so the display matches standard layout order
        whiteTakenPieces.sort(Comparator.comparingInt(Piece::getPieceValue));

        blackTakenPieces.sort(Comparator.comparingInt(Piece::getPieceValue));

        // Generate visual labels for White pieces captured by Black
        for (final Piece takenPiece : whiteTakenPieces) {
            try {
                final BufferedImage image = ImageIO.read(new File("art/pieces/" +
                        takenPiece.getPieceAlliance().toString().charAt(0) +
                        takenPiece + ".gif"));
                final ImageIcon icon = new ImageIcon(image);
                final JLabel imageLabel = new JLabel(icon);
                this.southPanel.add(imageLabel);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        // Generate visual labels for Black pieces captured by White
        for (final Piece takenPiece : blackTakenPieces) {
            try {
                final BufferedImage image = ImageIO.read(new File("art/pieces/" +
                        takenPiece.getPieceAlliance().toString().charAt(0) +
                        takenPiece + ".gif"));
                final ImageIcon icon = new ImageIcon(image);
                final JLabel imageLabel = new JLabel(icon);
                this.northPanel.add(imageLabel);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        validate();
        repaint();
    }
}