package com.AlgorithmSnake;
import javax.swing.*;
import java.awt.*;
import java.awt.Color;


public class MainMenu extends JPanel {
    private JFrame frame;

    public MainMenu (JFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(800, 600));
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JButton simulationButton = new JButton("Simulation");
        JButton playButton = new JButton("Play");
        JButton exitButton = new JButton("Exit");

        simulationButton.addActionListener(e -> startGame(false)); // AI only
        playButton.addActionListener(e -> startGame(true)); // Player mode
        exitButton.addActionListener(e -> System.exit(0));

        gbc.gridy = 0;
        add(simulationButton, gbc);
        gbc.gridy = 1;
        add(playButton, gbc);
        gbc.gridy = 2;
        add(exitButton, gbc);
    }

    private void startGame(boolean isPlayerMode) {
        frame.getContentPane().removeAll();
        AlgorithmSnake game = new AlgorithmSnake(isPlayerMode);
        frame.add(game);
        frame.pack();
        frame.revalidate();
        frame.repaint();
        game.requestFocusInWindow(); // Ensure key listener works
    }
}
