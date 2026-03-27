package game.component;

public class Key {

    public boolean isKey_enter() {
        return key_enter;
    }

    public void setKey_enter(boolean key_enter) {
        this.key_enter = key_enter;
    }

    public boolean isKey_right() {
        return key_right;
    }

    public void setKey_right(boolean key_right) {
        this.key_right = key_right;
    }

    public boolean isKey_left() {
        return key_left;
    }

    public void setKey_left(boolean key_left) {
        this.key_left = key_left;
    }

    public boolean isKey_w() {
        return key_w;
    }

    public void setKey_w(boolean key_w) {
        this.key_w = key_w;
    }

    public boolean isKey_j() {
        return key_j;
    }

    public void setKey_j(boolean key_j) {
        this.key_j = key_j;
    }

    public boolean isKey_k() {
        return key_k;
    }

    public void setKey_k(boolean key_k) {
        this.key_k = key_k;
    }

    private boolean key_right;
    private boolean key_left;
    private boolean key_w;
    private boolean key_j;
    private boolean key_k;
    private boolean key_enter;
}