package game.component;

import game.obj.Bullet;
import game.obj.Effect;
import game.obj.Player;
import game.obj.Rocket;
import game.obj.sound.Sound;
import game.obj.Background;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class SpaceWar extends JComponent {

    private Graphics2D g2;
    private BufferedImage image;
    private int width;
    private int height;
    private Thread gameThread;
    private boolean isRunning = true;
    private boolean isPause;
    private Key key;
    private int shotTime;

    // Game FPS
    private final int FPS = 60;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Game Object
    private Sound sound;
    private Player player;
    private List<Bullet> bullets;
    private List<Rocket> rockets;
    private List<Effect> boomEffects;
    private int score = 0;

    // พื้นหลัง
    private Background background;

    // เมนูระบบ
    private enum MenuState {
        MAIN_MENU,
        CONTROLS
    }

    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private GameState gameState = GameState.MENU;
    private MenuState menuState = MenuState.MAIN_MENU;
    private int selectedOption = 0;
    private String[] menuOptions = {"START GAME", "CONTROLS", "EXIT"};

    // Timer สำหรับ spawn rocket
    private javax.swing.Timer rocketSpawnTimer;

    public SpaceWar() {
        setFocusable(true);
        initGame();
        initKeyboard();
        startGameLoop();
    }

    private void initGame() {
        width = getWidth();
        height = getHeight();

        if (width == 0 || height == 0) {
            width = 1366;
            height = 768;
        }

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // สร้างพื้นหลัง
        background = new Background(width, height);

        initObjectGame();

        score = 0;
        isPause = false;
    }

    private void startGameLoop() {
        gameThread = new Thread(() -> {
            while (isRunning) {
                long startTime = System.nanoTime();

                // อัปเดตพื้นหลัง
                if (background != null) {
                    background.update();
                }

                // อัปเดตตามสถานะเกม
                switch (gameState) {
                    case PLAYING:
                        updateGame();
                        drawBackground();
                        drawGame();
                        break;
                    case PAUSED:
                        drawBackground();
                        drawPaused();
                        break;
                    case GAME_OVER:
                        drawBackground();
                        drawGameOver();
                        break;
                    case MENU:
                        drawBackground();
                        drawMenu();
                        break;
                }
                
                render();

                long time = System.nanoTime() - startTime;
                if (time < TARGET_TIME) {
                    long sleep = (TARGET_TIME - time) / 1000000;
                    sleep(sleep);
                }
            }
        });
        
        gameThread.start();
    }

    private void updateGame() {
        if (player.isAlive()) {
            float angle = player.getAngle();

            if (key.isKey_left()) {
                angle -= 2.5f;
            }
            if (key.isKey_right()) {
                angle += 2.5f;
            }

            if (key.isKey_j() || key.isKey_k()) {
                if (shotTime == 0) {
                    if (key.isKey_j()) {
                        bullets.add(0, new Bullet(player.getX(), player.getY(), player.getAngle(), 10, 8f));
                    } else {
                        bullets.add(0, new Bullet(player.getX(), player.getY(), player.getAngle(), 20, 5f));
                    }
                    if (sound != null) sound.soundShoot();
                }
                shotTime++;
                if (shotTime == 30) {
                    shotTime = 0;
                }
            } else {
                shotTime = 0;
            }

            if (key.isKey_w()) {
                player.speedUp();
            } else {
                player.speedDown();
            }

            player.update();

            double playerX = player.getX();
            double playerY = player.getY();
            if (playerX < 0) player.changeLocation(0, playerY);
            if (playerX > width - Player.PLAYER_SIZE) player.changeLocation(width - Player.PLAYER_SIZE, playerY);
            if (playerY < 0) player.changeLocation(playerX, 0);
            if (playerY > height - Player.PLAYER_SIZE) player.changeLocation(playerX, height - Player.PLAYER_SIZE);

            player.changeAngle(angle);
        } else {
            gameState = GameState.GAME_OVER;
            stopRocketSpawner();
        }

        // อัปเดต rockets
        for (int i = 0; i < rockets.size(); i++) {
            Rocket rocket = rockets.get(i);
            if (rocket != null) {
                if (player.isAlive()) {
                    double playerCenterX = player.getX() + Player.PLAYER_SIZE / 2;
                    double playerCenterY = player.getY() + Player.PLAYER_SIZE / 2;
                    rocket.trackPlayer(playerCenterX, playerCenterY);
                    rocket.update();
                }

                if (!rocket.check(width, height)) {
                    rockets.remove(rocket);
                    i--;
                } else {
                    if (player.isAlive()) {
                        checkPlayer(rocket);
                    }
                }
            }
        }

        // อัปเดต bullets
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            if (bullet != null) {
                bullet.update();
                checkBullets(bullet);
                if (!bullet.check(width, height)) {
                    bullets.remove(bullet);
                    i--;
                }
            }
        }

        // อัปเดต effects
        for (int i = 0; i < boomEffects.size(); i++) {
            Effect boomEffect = boomEffects.get(i);
            if (boomEffect != null) {
                boomEffect.update();
                if (!boomEffect.check()) {
                    boomEffects.remove(boomEffect);
                    i--;
                }
            }
        }
    }

    private void startGame() {
        resetGame();
        gameState = GameState.PLAYING;
        startRocketSpawner();
    }

    private void startRocketSpawner() {
        if (rocketSpawnTimer != null) {
            rocketSpawnTimer.stop();
        }

        rocketSpawnTimer = new javax.swing.Timer(3000, (e) -> {
            if (gameState == GameState.PLAYING && player.isAlive()) {
                addRocket();
            }
        });
        rocketSpawnTimer.start();
    }

    private void stopRocketSpawner() {
        if (rocketSpawnTimer != null) {
            rocketSpawnTimer.stop();
        }
    }

    private void addRocket() {
        Random ran = new Random();

        if (rockets.size() < 10) {
            int locationY = ran.nextInt(Math.max(1, height - 50)) + 25;
            Rocket rocket = new Rocket();
            rocket.changeLocation(0, locationY);
            rocket.changeAngle(0);
            rockets.add(rocket);
        }

        if (rockets.size() < 10) {
            int locationY2 = ran.nextInt(Math.max(1, height - 50)) + 25;
            Rocket rocket2 = new Rocket();
            rocket2.changeLocation(width, locationY2);
            rocket2.changeAngle(180);
            rockets.add(rocket2);
        }
    }

    private void initObjectGame() {
        sound = new Sound();
        player = new Player();
        player.changeLocation(150, 150);
        rockets = new ArrayList<>();
        boomEffects = new ArrayList<>();
        bullets = new ArrayList<>();
    }

    private void resetGame() {
        score = 0;
        rockets.clear();
        bullets.clear();
        boomEffects.clear();
        player.changeLocation(150, 150);
        player.reset();
        player.setAlive(true);
    }

    private void initKeyboard() {
        key = new Key();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // จัดการตามสถานะเกม
                switch (gameState) {
                    case MENU:
                        handleMenuInput(e);
                        break;
                    case PLAYING:
                        handlePlayingInput(e);
                        break;
                    case PAUSED:
                        handlePausedInput(e);
                        break;
                    case GAME_OVER:
                        handleGameOverInput(e);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (gameState == GameState.PLAYING) {
                    if (e.getKeyCode() == KeyEvent.VK_A) {
                        key.setKey_left(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        key.setKey_right(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_W) {
                        key.setKey_w(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_J) {
                        key.setKey_j(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        key.setKey_k(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        key.setKey_enter(false);
                    }
                }
            }
        });
    }

    private void handleMenuInput(KeyEvent e) {
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
                        startGame();
                        break;
                    case 1:  // CONTROLS
                        menuState = MenuState.CONTROLS;
                        break;
                    case 2:  // EXIT
                        System.exit(0);
                        break;
                }
                break;

            case KeyEvent.VK_ESCAPE:
                if (menuState == MenuState.CONTROLS) {
                    menuState = MenuState.MAIN_MENU;
                }
                break;
        }
    }

    private void handlePlayingInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) {
            key.setKey_left(true);
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            key.setKey_right(true);
        } else if (e.getKeyCode() == KeyEvent.VK_W) {
            key.setKey_w(true);
        } else if (e.getKeyCode() == KeyEvent.VK_J) {
            key.setKey_j(true);
        } else if (e.getKeyCode() == KeyEvent.VK_K) {
            key.setKey_k(true);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            key.setKey_enter(true);
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameState = GameState.PAUSED;
        }
    }

    private void handlePausedInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameState = GameState.PLAYING;
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            gameState = GameState.PLAYING;
        }
    }

    private void handleGameOverInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            gameState = GameState.MENU;
            menuState = MenuState.MAIN_MENU;
            selectedOption = 0;
            resetGame();
        }
    }

    private void checkBullets(Bullet bullet) {
        for (int i = 0; i < rockets.size(); i++) {
            Rocket rocket = rockets.get(i);
            if (rocket != null) {
                Area area = new Area(bullet.getShape());
                area.intersect(rocket.getShape());
                if (!area.isEmpty()) {
                    if (boomEffects != null) {
                        boomEffects.add(new Effect(bullet.getCenterX(), bullet.getCenterY(), 3, 5, 60, 1f, new Color(230, 207, 105)));
                    }
                    if (!rocket.updateHP(bullet.getSize())) {
                        score++;
                        rockets.remove(rocket);
                        if (sound != null) sound.soundDestroy();
                        double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                        double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                        if (boomEffects != null) {
                            boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 0, 0)));
                            boomEffects.add(new Effect(x, y, 5, 5, 50, 0.4f, new Color(255, 75, 0)));
                            boomEffects.add(new Effect(x, y, 10, 10, 75, 0.8f, new Color(230, 207, 105)));
                            boomEffects.add(new Effect(x, y, 10, 5, 100, 1.2f, new Color(255, 255, 255)));
                        }
                    } else {
                        if (sound != null) sound.soundHit();
                    }
                    bullets.remove(bullet);
                    return;
                }
            }
        }
    }

    private void checkPlayer(Rocket rocket) {
        if (rocket != null) {
            Area area = new Area(player.getShape());
            area.intersect(rocket.getShape());
            if (!area.isEmpty()) {
                double rocketHp = rocket.getHP();
                if (!rocket.updateHP(player.getHP())) {
                    rockets.remove(rocket);
                    if (sound != null) sound.soundDestroy();
                    double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                    double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                    if (boomEffects != null) {
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 0, 0)));
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.4f, new Color(255, 75, 0)));
                        boomEffects.add(new Effect(x, y, 10, 10, 75, 0.8f, new Color(230, 207, 105)));
                        boomEffects.add(new Effect(x, y, 10, 5, 100, 1.2f, new Color(255, 255, 255)));
                    }
                }
                if (!player.updateHP(rocketHp)) {
                    player.setAlive(false);
                    if (sound != null) sound.soundDestroy();
                    double x = player.getX() + Player.PLAYER_SIZE / 2;
                    double y = player.getY() + Player.PLAYER_SIZE / 2;
                    if (boomEffects != null) {
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.1f, new Color(255, 0, 0)));
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 75, 0)));
                        boomEffects.add(new Effect(x, y, 10, 10, 75, 0.4f, new Color(230, 207, 105)));
                        boomEffects.add(new Effect(x, y, 10, 5, 100, 0.6f, new Color(255, 255, 255)));
                    }
                }
            }
        }
    }

    private void drawBackground() {
        if (background != null) {
            background.draw(g2);
        } else {
            // Fallback ถ้าไม่มี Background
            g2.setColor(new Color(30, 30, 30));
            g2.fillRect(0, 0, width, height);
        }
    }

    private void drawMenu() {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, width, height);

        if (menuState == MenuState.MAIN_MENU) {
            // Title
            g2.setFont(getFont().deriveFont(Font.BOLD, 48f));
            g2.setColor(new Color(255, 200, 0));
            String title = "SPACE WAR";
            FontMetrics titleFm = g2.getFontMetrics();
            int titleX = (width - titleFm.stringWidth(title)) / 2;
            g2.drawString(title, titleX, height / 2 - 100);

            // Menu options
            g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
            for (int i = 0; i < menuOptions.length; i++) {
                if (i == selectedOption) {
                    g2.setColor(new Color(255, 200, 0));
                    g2.drawString("> " + menuOptions[i] + " <", width / 2 - 110, height / 2 + i * 40);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.drawString(menuOptions[i], width / 2 - 90, height / 2 + i * 40);
                }
            }

        } else if (menuState == MenuState.CONTROLS) {
            // Controls screen
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 36f));
            String controlsTitle = "CONTROLS";
            FontMetrics controlsTitleFm = g2.getFontMetrics();
            int titleX = (width - controlsTitleFm.stringWidth(controlsTitle)) / 2;
            g2.drawString(controlsTitle, titleX, height / 2 - 100);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
            String[] controls = {
                    "A / D : Rotate ship",
                    "W : Boost",
                    "J : Shoot bullet",
                    "K : Shoot heavy bullet",
                    "ESC : Pause game",
                    "ENTER : Start / Restart"
            };

            int y = height / 2 - 20;
            for (String control : controls) {
                FontMetrics controlFm = g2.getFontMetrics();
                int x = (width - controlFm.stringWidth(control)) / 2;
                g2.drawString(control, x, y);
                y += 35;
            }

            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            String backMsg = "Press ESC to return to menu";
            FontMetrics backFm = g2.getFontMetrics();
            int backX = (width - backFm.stringWidth(backMsg)) / 2;
            g2.drawString(backMsg, backX, height - 50);

        }
    }

    private void drawGame() {
        if (player.isAlive()) {
            player.draw(g2);
        }

        if (bullets != null) {
            for (Bullet bullet : bullets) {
                if (bullet != null) {
                    bullet.draw(g2);
                }
            }
        }

        if (rockets != null) {
            for (Rocket rocket : rockets) {
                if (rocket != null) {
                    rocket.draw(g2);
                }
            }
        }

        if (boomEffects != null) {
            for (Effect effect : boomEffects) {
                if (effect != null) {
                    effect.draw(g2);
                }
            }
        }

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.drawString("Score : " + score, 10, 20);

        //สร้างหลิดเลือด
        if (player != null) {
            g2.setColor(Color.RED);
            g2.fillRect(10, 30, 200, 20);
            g2.setColor(Color.GREEN);

            //คำนวนหลอดเลือดเป็น%
            double healthPercent = player.getHP() / 100.0; 
            if (healthPercent < 0) healthPercent = 0;
            if (healthPercent > 1) healthPercent = 1;

            int healthWidth = (int)(200 * healthPercent);
            g2.fillRect(10, 30, healthWidth, 20);
            g2.setColor(Color.WHITE);
            g2.drawRect(10, 30, 200, 20);

            //เพิ่มแสดง%ที่หลอดเลือด
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            String healthText = (int)(healthPercent * 100) + "%";
            FontMetrics fm = g2.getFontMetrics();
            int textX = 10 + (200 - fm.stringWidth(healthText)) / 2;
            g2.drawString(healthText, textX, 45);
        }
    }
    //กด esc แสดงหน้าหยุดเกม
    private void drawPaused() {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 40f));
        String text = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, height / 2);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 18f));
        String resumeMsg = "Press ESC or ENTER to resume";
        fm = g2.getFontMetrics();
        int resumeX = (width - fm.stringWidth(resumeMsg)) / 2;
        g2.drawString(resumeMsg, resumeX, height / 2 + 50);
    }
    //เลือดผู้เล่นเหลือ0แสดงจบเกม
    private void drawGameOver() {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.RED);
        g2.setFont(getFont().deriveFont(Font.BOLD, 50f));
        String text = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, height / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
        String scoreText = "Your Score: " + score;
        fm = g2.getFontMetrics();
        int scoreX = (width - fm.stringWidth(scoreText)) / 2;
        g2.drawString(scoreText, scoreX, height / 2 + 20);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
        String restartMsg = "Press ENTER to return to menu";
        fm = g2.getFontMetrics();
        int restartX = (width - fm.stringWidth(restartMsg)) / 2;
        g2.drawString(restartMsg, restartX, height / 2 + 80);
    }

    private void render() {
        Graphics g = getGraphics();
        if (g != null && image != null) {
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }

    private void sleep(long speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            repaint();
        });
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        isRunning = false;
        if (rocketSpawnTimer != null) rocketSpawnTimer.stop();
        if (gameThread != null) gameThread.interrupt();
        if (g2 != null) g2.dispose();
    }
}
