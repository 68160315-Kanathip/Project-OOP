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

public class SpaceWar extends JComponent {

    private Graphics2D g2;
    private BufferedImage image;
    private int width;
    private int height;
    private Thread thread;
    private boolean start = true;
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

    // เพิ่มพื้นหลัง
    private Background background;

    // ตัวแปรสำหรับเมนู
    private boolean gameStarted = false;
    private boolean showMenu = true;

    public SpaceWar() {
        setFocusable(true);
        initGame();
        initKeyboard();
        initBullets();
        startGameLoop();

        // ตรวจสอบการเริ่มเกม
        new Thread(() -> {
            while (start) {
                if (showMenu) {
                    if (key != null && key.isKey_enter()) {
                        showMenu = false;
                        gameStarted = true;
                        resetGame();
                        key.setKey_enter(false);
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (start) {
                    long startTime = System.nanoTime();

                    // อัปเดตพื้นหลัง
                    if (background != null) {
                        background.update();
                    }

                    if (gameStarted && !showMenu) {
                        updateGame();
                        drawBackground();
                        drawGame();
                    } else {
                        drawBackground();
                        drawMenu();
                    }

                    render();

                    long time = System.nanoTime() - startTime;
                    if (time < TARGET_TIME) {
                        long sleep = (TARGET_TIME - time) / 1000000;
                        sleep(sleep);
                    }
                }
            }
        });
        thread.start();
    }

    private void updateGame() {
        if (!isPause) {
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
                        sound.soundShoot();
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
                if (key.isKey_enter()) {
                    resetGame();
                }
            }

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (start) {
                    if (!isPause && gameStarted && !showMenu) {
                        addRocket();
                    }
                    sleep(3000);
                }
            }
        }).start();
    }

    private void initBullets() {
        System.out.println("Bullet system initialized");
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
                    if (gameStarted && !showMenu && player.isAlive()) {
                        isPause = !isPause;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
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
        });
    }

    private void checkBullets(Bullet bullet) {
        for (int i = 0; i < rockets.size(); i++) {
            Rocket rocket = rockets.get(i);
            if (rocket != null) {
                Area area = new Area(bullet.getShape());
                area.intersect(rocket.getShape());
                if (!area.isEmpty()) {
                    boomEffects.add(new Effect(bullet.getCenterX(), bullet.getCenterY(), 3, 5, 60, 1f, new Color(230, 207, 105)));
                    if (!rocket.updateHP(bullet.getSize())) {
                        score++;
                        rockets.remove(rocket);
                        sound.soundDestroy();
                        double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                        double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 0, 0)));
                        boomEffects.add(new Effect(x, y, 5, 5, 50, 0.4f, new Color(255, 75, 0)));
                        boomEffects.add(new Effect(x, y, 10, 10, 75, 0.8f, new Color(230, 207, 105)));
                        boomEffects.add(new Effect(x, y, 10, 5, 100, 1.2f, new Color(255, 255, 255)));
                    } else {
                        sound.soundHit();
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
                    sound.soundDestroy();
                    double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                    double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                    boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 0, 0)));
                    boomEffects.add(new Effect(x, y, 5, 5, 50, 0.4f, new Color(255, 75, 0)));
                    boomEffects.add(new Effect(x, y, 10, 10, 75, 0.8f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 10, 5, 100, 1.2f, new Color(255, 255, 255)));
                }
                if (!player.updateHP(rocketHp)) {
                    player.setAlive(false);
                    sound.soundDestroy();
                    double x = player.getX() + Player.PLAYER_SIZE / 2;
                    double y = player.getY() + Player.PLAYER_SIZE / 2;
                    boomEffects.add(new Effect(x, y, 5, 5, 50, 0.1f, new Color(255, 0, 0)));
                    boomEffects.add(new Effect(x, y, 5, 5, 50, 0.2f, new Color(255, 75, 0)));
                    boomEffects.add(new Effect(x, y, 10, 10, 75, 0.4f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 10, 5, 100, 0.6f, new Color(255, 255, 255)));
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
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, width, height);

        g2.setFont(getFont().deriveFont(Font.BOLD, 48f));
        g2.setColor(new Color(255, 200, 0));
        String title = "SPACE WAR";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, height / 2 - 100);

        g2.setFont(getFont().deriveFont(Font.BOLD, 20f));
        g2.setColor(Color.WHITE);
        String startMsg = "Press ENTER to Start Game";
        String controlsMsg = "Controls: A / D to turn , W to move , J / K to shoot";

        fm = g2.getFontMetrics();
        int startX = (width - fm.stringWidth(startMsg)) / 2;
        int controlsX = (width - fm.stringWidth(controlsMsg)) / 2;

        g2.drawString(startMsg, startX, height / 2);
        g2.drawString(controlsMsg, controlsX, height / 2 + 50);

        if (System.currentTimeMillis() % 1000 < 500) {
            g2.setColor(new Color(255, 200, 0));
            g2.drawString("▶", startX - 30, height / 2);
        }
    }

    private void drawGame() {
        if (player.isAlive()) {
            player.draw(g2);
        }

        for (Bullet bullet : bullets) {
            if (bullet != null) {
                bullet.draw(g2);
            }
        }

        for (Rocket rocket : rockets) {
            if (rocket != null) {
                rocket.draw(g2);
            }
        }

        for (Effect effect : boomEffects) {
            if (effect != null) {
                effect.draw(g2);
            }
        }

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.drawString("Score : " + score, 10, 20);

        if (!player.isAlive()) {
            String text = "GAME OVER";
            String textKey = "Press ENTER to Restart";
            g2.setFont(getFont().deriveFont(Font.BOLD, 50f));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (width - fm.stringWidth(text)) / 2;
            g2.drawString(text, textX, height / 2 - 50);

            g2.setFont(getFont().deriveFont(Font.BOLD, 20f));
            fm = g2.getFontMetrics();
            int textKeyX = (width - fm.stringWidth(textKey)) / 2;
            g2.drawString(textKey, textKeyX, height / 2 + 50);
        }

        if (isPause) {
            String text = "PAUSED";
            g2.setFont(getFont().deriveFont(Font.BOLD, 40f));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (width - fm.stringWidth(text)) / 2;
            g2.drawString(text, textX, height / 2);
        }
    }

    private void render() {
        Graphics g = getGraphics();
        if (g != null) {
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
    }

    private void sleep(long speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }
}