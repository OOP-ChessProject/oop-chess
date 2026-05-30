package com.chess.gui;

import com.chess.engine.Alliance;
import com.chess.engine.player.Player;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameSetup extends JDialog {

    private PlayerType whitePlayerType;
    private PlayerType blackPlayerType;
    private int searchDepth;

    private static final String HUMAN_TEXT = "Human";
    private static final String COMPUTER_TEXT = "AI Engine";

    public GameSetup(final JFrame frame, final boolean modal) {
        super(frame, modal);
        final JPanel myPanel = new JPanel(new GridLayout(0, 1));
        final JRadioButton whiteHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton whiteComputerButton = new JRadioButton(COMPUTER_TEXT);
        final JRadioButton blackHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton blackComputerButton = new JRadioButton(COMPUTER_TEXT);

        whiteHumanButton.setSelected(true);
        blackHumanButton.setSelected(true);

        final ButtonGroup whiteGroup = new ButtonGroup();
        whiteGroup.add(whiteHumanButton);
        whiteGroup.add(whiteComputerButton);

        final ButtonGroup blackGroup = new ButtonGroup();
        blackGroup.add(blackHumanButton);
        blackGroup.add(blackComputerButton);

        myPanel.add(new JLabel("White Player Config:"));
        myPanel.add(whiteHumanButton);
        myPanel.add(whiteComputerButton);
        myPanel.add(new JLabel("Black Player Config:"));
        myPanel.add(blackHumanButton);
        myPanel.add(blackComputerButton);

        myPanel.add(new JLabel("AI Engine Search Depth (MiniMax):"));
        final JSlider searchDepthSlider = createSearchDepthSlider();
        myPanel.add(searchDepthSlider);

        final JButton cancelButton = new JButton("Cancel");
        final JButton okButton = new JButton("Apply Setup");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                whitePlayerType = whiteHumanButton.isSelected() ? PlayerType.HUMAN : PlayerType.COMPUTER;
                blackPlayerType = blackHumanButton.isSelected() ? PlayerType.HUMAN : PlayerType.COMPUTER;
                searchDepth = searchDepthSlider.getValue();
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        this.getContentPane().add(myPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(frame);
    }

    /**
     * Helper configuration to style and tick the search depth controller.
     */
    private JSlider createSearchDepthSlider() {
        final JSlider searchDepthSlider = new JSlider(JSlider.HORIZONTAL, 1, 6, 4);
        searchDepthSlider.setMajorTickSpacing(1);
        searchDepthSlider.setPaintTicks(true);
        searchDepthSlider.setPaintLabels(true);
        return searchDepthSlider;
    }

    /**
     * Opens the setup prompt modal to pause game execution while changing parameters.
     */
    public void promptUser() {
        this.setVisible(true);
    }

    public boolean isAIPlayer(final Player player) {
        if (player.getAlliance() == Alliance.WHITE) {
            return getWhitePlayerType() == PlayerType.COMPUTER;
        }
        return getBlackPlayerType() == PlayerType.COMPUTER;
    }

    public PlayerType getWhitePlayerType() {
        return this.whitePlayerType;
    }

    public PlayerType getBlackPlayerType() {
        return this.blackPlayerType;
    }

    public int getSearchDepth() {
        return this.searchDepth;
    }

    /**
     * Enumeration tracking player control variants.
     */
    public enum PlayerType {
        HUMAN,
        COMPUTER
    }
}