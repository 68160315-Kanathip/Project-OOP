package game.obj;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import javax.swing.ImageIcon;

public class Rocket extends HpRender {

    public Rocket() {
        super(new HP(30, 30));
        this.image = new ImageIcon(getClass().getResource("/game/image/rocket.png")).getImage();
        Path2D p = new Path2D.Double();
        p.moveTo(0, ROCKET_SIZE / 2);
        p.lineTo(15, 10);
        p.lineTo(ROCKET_SIZE - 5, 13);
        p.lineTo(ROCKET_SIZE + 10, ROCKET_SIZE / 2);
        p.lineTo(ROCKET_SIZE - 5, ROCKET_SIZE - 13);
        p.lineTo(15, ROCKET_SIZE - 10);
        rocketShap = new Area(p);
    }

    public static final double ROCKET_SIZE = 50;
    private double x;
    private double y;
    private final float speed = 1.2f;  // เพิ่มความเร็วเล็กน้อย
    private float angle = 0;
    private final Image image;
    private final Area rocketShap;

    // เพิ่มตัวแปรสำหรับติดตาม
    private boolean isTracking = true;  // เปิดโหมดติดตาม
    private float trackingSpeed = 1f;   // ความเร็วในการหมุนติดตาม

    public void changeLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        x += Math.cos(Math.toRadians(angle)) * speed;
        y += Math.sin(Math.toRadians(angle)) * speed;
    }

    // เพิ่มเมธอดสำหรับติดตามผู้เล่น
    public void trackPlayer(double playerX, double playerY) {
        if (isTracking) {
            // คำนวณมุมไปหาผู้เล่น
            double dx = playerX - (x + ROCKET_SIZE/2);
            double dy = playerY - (y + ROCKET_SIZE/2);
            float targetAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

            // ค่อยๆ หมุนเข้าหาผู้เล่น (ไม่หมุนทันที)
            float angleDiff = targetAngle - angle;

            // ปรับมุมให้อยู่ในช่วง -180 ถึง 180
            if (angleDiff > 180) angleDiff -= 360;
            if (angleDiff < -180) angleDiff += 360;

            // จำกัดความเร็วในการหมุน
            if (angleDiff > trackingSpeed) {
                angleDiff = trackingSpeed;
            } else if (angleDiff < -trackingSpeed) {
                angleDiff = -trackingSpeed;
            }

            changeAngle(angle + angleDiff);
        }
    }

    public void changeAngle(float angle) {
        if (angle < 0) {
            angle = 359;
        } else if (angle > 359) {
            angle = 0;
        }
        this.angle = angle;
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(x, y);
        AffineTransform tran = new AffineTransform();
        tran.rotate(Math.toRadians(angle + 45), ROCKET_SIZE / 2, ROCKET_SIZE / 2);
        g2.drawImage(image, tran, null);
        Shape shap = getShape();
        hpRender(g2, shap, y);
        g2.setTransform(oldTransform);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }

    public Area getShape() {
        AffineTransform afx = new AffineTransform();
        afx.translate(x, y);
        afx.rotate(Math.toRadians(angle), ROCKET_SIZE / 2, ROCKET_SIZE / 2);
        return new Area(afx.createTransformedShape(rocketShap));
    }

    public boolean check(int width, int height) {
        Rectangle size = getShape().getBounds();
        if (x <= -size.getWidth() || y < -size.getHeight() || x > width || y > height) {
            return false;
        } else {
            return true;
        }
    }

    // Getter/Setter สำหรับการติดตาม
    public void setTracking(boolean tracking) {
        this.isTracking = tracking;
    }

    public void setTrackingSpeed(float speed) {
        this.trackingSpeed = speed;
    }
}