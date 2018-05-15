package cs.ubbcluj.rosana.cleanphoto;

/**
 * Created by rosana on 15.05.2018.
 */

public class YUVPixel {
    int y;
    int u;
    int v;

    public YUVPixel(int y, int u, int v) {
        this.y = y;
        this.u = u;
        this.v = v;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getU() {
        return u;
    }

    public void setU(int u) {
        this.u = u;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }
}
