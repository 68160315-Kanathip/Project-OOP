package game.obj;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Background {

    private int width;
    private int height;
    private List<Star> stars;
    private List<ShootingStar> shootingStars;
    private Random random;
    private float offsetX = 0;
    private float offsetY = 0;

    // คลาสสำหรับดาว
    private class Star {
        float x, y;
        int size;
        int alpha;
        float speed;

        Star(float x, float y, int size, int alpha, float speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.alpha = alpha;
            this.speed = speed;
        }

        void update() {
            y += speed;
            if (y > height) {
                y = 0;
                x = random.nextInt(width);
            }
            // ดาวกระพริบ
            alpha = 100 + random.nextInt(155);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(255, 255, 255, Math.min(255, Math.max(0, alpha))));
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    // คลาสสำหรับดาวตก
    private class ShootingStar {
        float x, y;
        float length;
        int alpha;
        float speed;
        boolean active;

        ShootingStar() {
            reset();
        }

        void reset() {
            x = random.nextInt(width);
            y = random.nextInt(Math.max(1, height / 3));
            length = 30 + random.nextInt(50);
            alpha = 150 + random.nextInt(105);
            speed = 5 + random.nextInt(10);
            active = true;
        }

        void update() {
            if (active) {
                x += speed;
                y += speed * 0.5f;
                alpha -= 5;

                if (alpha < 0) alpha = 0;

                if (x > width + 100 || y > height + 100 || alpha <= 0) {
                    active = false;
                }
            } else {
                // สุ่มทำให้ active อีกครั้ง
                if (random.nextInt(500) == 0) {
                    active = true;
                    reset();
                }
            }
        }

        void draw(Graphics2D g2) {
            if (active && alpha > 0) {
                int step = Math.max(1, (int)length / 15);
                for (int i = 0; i < length; i += step) {
                    int alpha2 = alpha - (i * (alpha / (int)Math.max(1, length)));
                    if (alpha2 > 0) {
                        g2.setColor(new Color(255, 255, 200, Math.min(255, Math.max(0, alpha2))));
                        g2.drawLine((int)(x - i * 2), (int)(y - i),
                                (int)(x - (i + 1) * 2), (int)(y - (i + 1)));
                    }
                }
            }
        }
    }

    public Background(int width, int height) {
        this.width = width;
        this.height = height;
        this.random = new Random();
        this.stars = new ArrayList<>();
        this.shootingStars = new ArrayList<>();

        initStars();
        initShootingStars();
    }

    private void initStars() {
        // สร้างดาว 150 ดวง
        for (int i = 0; i < 50; i++) {
            float x = random.nextInt(width);
            float y = random.nextInt(height);
            int size = random.nextInt(3) + 1;
            int alpha = random.nextInt(155) + 100;
            float speed = 0.2f + random.nextFloat() * 0.5f;
            stars.add(new Star(x, y, size, alpha, speed));
        }
    }

    private void initShootingStars() {
        // สร้างดาวตก 3 ดวง
        for (int i = 0; i < 5; i++) {
            shootingStars.add(new ShootingStar());
        }
    }

    public void update() {
        // อัปเดตดาว
        for (Star star : stars) {
            star.update();
        }

        // อัปเดตดาวตก
        for (ShootingStar ss : shootingStars) {
            ss.update();
        }

        // เคลื่อนที่พื้นหลังช้าๆ
        offsetX += 0.03f;
        if (offsetX > width) offsetX = 0;
        offsetY += 0.02f;
        if (offsetY > height) offsetY = 0;
    }

    public void draw(Graphics2D g2) {
        // วาดพื้นหลังไล่สี
        drawGradientBackground(g2);

        // วาดดาว
        for (Star star : stars) {
            star.draw(g2);
        }

        // วาดดาวตก
        for (ShootingStar ss : shootingStars) {
            ss.draw(g2);
        }
    }

    private void drawGradientBackground(Graphics2D g2) {
        // สร้างพื้นหลังไล่สีจากเข้มไปสว่าง
        for (int i = 0; i < height; i++) {
            float ratio = (float) i / height;
            // สีน้ำเงินเข้มไปจนถึงม่วง
            int r = (int) (0 + 5 * ratio);
            int gr = (int) (0 + 0 * ratio);
            int b = (int) (0 + 5 * ratio);

            g2.setColor(new Color(r, gr, b));
            g2.drawLine(0, i, width, i);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        // รีเซ็ตพื้นหลังใหม่
        stars.clear();
        shootingStars.clear();
        initStars();
        initShootingStars();
    }
}