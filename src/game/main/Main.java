package game.main;

import game.component.SpaceWar;
import javax.swing.JFrame;
import java.awt.BorderLayout;

public class Main extends JFrame {

    private SpaceWar gamePanel;

    public Main() {
        init();
    }

    private void init() {
        setTitle("Space War");
        setSize(1366, 768);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new SpaceWar();
        add(gamePanel, BorderLayout.CENTER);

        setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        new Main();
    }
}