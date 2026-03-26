package game.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;

public class GameMenu extends JComponent {

    private enum MenuState {
        MAIN,      // เมนูหลัก
        CONTROLS,  // หน้าวิธีการควบคุม
        EXIT       // ออกจากเกม
    }

    private MenuState currentState = MenuState.MAIN;
    private int selectedOption = 0;  // 0=Start, 1=Controls, 2=Exit
    private String[] menuOptions = {"START GAME", "CONTROLS", "EXIT"};
    private boolean startGame = false;

    public GameMenu() {
        setFocusable(true);
        requestFocus();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (currentState) {
                    case MAIN:
                        handleMainMenu(e);
                        break;
                    case CONTROLS:
                        if (e.getKeyCode() == KeyEvent.VK_ESCAPE ||
                                e.getKeyCode() == KeyEvent.VK_ENTER) {
                            currentState = MenuState.MAIN;
                        }
                        break;
                    case EXIT:
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            System.exit(0);
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            currentState = MenuState.MAIN;
                            selectedOption = 2;
                        }
                        break;
                }
                repaint();
            }
        });
    }

    private void handleMainMenu(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                selectedOption--;
                if (selectedOption < 0) {
                    selectedOption = menuOptions.length - 1;
                }
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                selectedOption++;
                if (selectedOption >= menuOptions.length) {
                    selectedOption = 0;
                }
                break;

            case KeyEvent.VK_ENTER:
                switch (selectedOption) {
                    case 0:  // START GAME
                        startGame = true;
                        break;
                    case 1:  // CONTROLS
                        currentState = MenuState.CONTROLS;
                        break;
                    case 2:  // EXIT
                        currentState = MenuState.EXIT;
                        break;
                }
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // วาดพื้นหลัง
        drawBackground(g2);

        // วาดตามสถานะ
        switch (currentState) {
            case MAIN:
                drawMainMenu(g2);
                break;
            case CONTROLS:
                drawControls(g2);
                break;
            case EXIT:
                drawExitConfirm(g2);
                break;
        }
    }

    private void drawBackground(Graphics2D g2) {
        // สร้างเอฟเฟกต์ไล่สีพื้นหลัง
        Color startColor = new Color(10, 10, 30);
        Color endColor = new Color(20, 20, 50);

        for (int i = 0; i < getHeight(); i++) {
            float ratio = (float) i / getHeight();
            int r = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int gr = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int b = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);
            g2.setColor(new Color(r, gr, b));
            g2.drawLine(0, i, getWidth(), i);
        }

        // วาดดาวตก
        g2.setColor(Color.WHITE);
        for (int i = 0; i < 100; i++) {
            int x = (int) (Math.random() * getWidth());
            int y = (int) (Math.random() * getHeight());
            g2.fillOval(x, y, 2, 2);
        }
    }

    private void drawMainMenu(Graphics2D g2) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // วาดชื่อเกม
        Font titleFont = new Font("Monospaced", Font.BOLD, 60);
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 200, 0));
        String title = "SPACE WAR";
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D titleBounds = fm.getStringBounds(title, g2);
        int titleX = centerX - (int) titleBounds.getWidth() / 2;
        int titleY = centerY - 150;

        // เพิ่มเงาชื่อเกม
        g2.setColor(new Color(100, 50, 0));
        g2.drawString(title, titleX + 3, titleY + 3);
        g2.setColor(new Color(255, 200, 0));
        g2.drawString(title, titleX, titleY);

        // วาดเส้นแบ่ง
        g2.setColor(new Color(255, 200, 0, 100));
        g2.fillRect(centerX - 150, titleY + 20, 300, 2);

        // วาดเมนูตัวเลือก
        Font menuFont = new Font("Monospaced", Font.PLAIN, 30);
        g2.setFont(menuFont);

        for (int i = 0; i < menuOptions.length; i++) {
            int y = centerY + (i * 50);

            // ตัวเลือกที่ถูกเลือก
            if (i == selectedOption) {
                g2.setColor(new Color(255, 200, 0));
                g2.fillRect(centerX - 130, y - 20, 260, 40);
                g2.setColor(Color.BLACK);
            } else {
                g2.setColor(Color.WHITE);
            }

            fm = g2.getFontMetrics();
            Rectangle2D bounds = fm.getStringBounds(menuOptions[i], g2);
            int x = centerX - (int) bounds.getWidth() / 2;
            g2.drawString(menuOptions[i], x, y);
        }

        // วาดคำแนะนำ
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.setColor(new Color(150, 150, 150));
        String controls = "Use UP/DOWN or W/S to navigate | ENTER to select";
        fm = g2.getFontMetrics();
        int controlsX = centerX - (int) fm.getStringBounds(controls, g2).getWidth() / 2;
        g2.drawString(controls, controlsX, getHeight() - 50);
    }

    private void drawControls(Graphics2D g2) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // หัวข้อ
        Font titleFont = new Font("Monospaced", Font.BOLD, 40);
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 200, 0));
        String title = "CONTROLS";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - (int) fm.getStringBounds(title, g2).getWidth() / 2;
        g2.drawString(title, titleX, centerY - 150);

        // รายการปุ่มควบคุม
        Font controlFont = new Font("Monospaced", Font.PLAIN, 20);
        g2.setFont(controlFont);
        g2.setColor(Color.WHITE);

        String[] controls = {
                "MOVE LEFT  :  A",
                "MOVE RIGHT :  D",
                "MOVE UP    :  W",
                "SHOOT (small) : J",
                "SHOOT (big)   : K",
                "PAUSE      :  ESC",
                "SELECT     :  ENTER"
        };

        int startY = centerY - 80;
        for (int i = 0; i < controls.length; i++) {
            int y = startY + (i * 35);
            g2.drawString(controls[i], centerX - 150, y);
        }

        // คำแนะนำการกลับ
        Font backFont = new Font("Monospaced", Font.PLAIN, 16);
        g2.setFont(backFont);
        g2.setColor(new Color(150, 150, 150));
        String backMsg = "Press ENTER or ESC to return to main menu";
        fm = g2.getFontMetrics();
        int backX = centerX - (int) fm.getStringBounds(backMsg, g2).getWidth() / 2;
        g2.drawString(backMsg, backX, getHeight() - 50);
    }

    private void drawExitConfirm(Graphics2D g2) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // พื้นหลังโปร่งแสง
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // กล่องข้อความ
        g2.setColor(new Color(50, 50, 70));
        g2.fillRoundRect(centerX - 200, centerY - 80, 400, 160, 20, 20);
        g2.setColor(new Color(255, 200, 0));
        g2.drawRoundRect(centerX - 200, centerY - 80, 400, 160, 20, 20);

        // ข้อความ
        Font font = new Font("Monospaced", Font.BOLD, 24);
        g2.setFont(font);
        g2.setColor(Color.WHITE);
        String msg = "Exit Game?";
        FontMetrics fm = g2.getFontMetrics();
        int msgX = centerX - (int) fm.getStringBounds(msg, g2).getWidth() / 2;
        g2.drawString(msg, msgX, centerY - 30);

        Font smallFont = new Font("Monospaced", Font.PLAIN, 18);
        g2.setFont(smallFont);
        String confirm = "Press ENTER to exit | ESC to cancel";
        fm = g2.getFontMetrics();
        int confirmX = centerX - (int) fm.getStringBounds(confirm, g2).getWidth() / 2;
        g2.drawString(confirm, confirmX, centerY + 30);
    }

    public boolean isStartGame() {
        return startGame;
    }
}