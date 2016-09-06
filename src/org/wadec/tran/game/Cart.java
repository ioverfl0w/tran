package org.wadec.tran.game;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

/**
 *
 * @author wadec
 */
public class Cart {

    public int score = 0;
    private Point pos;
    private final ArrayList<Point> trail = new ArrayList<>();
    public final Color color;

    public Cart(Point p, Color c) {
        pos = p;
        color = c;
    }

    public int getX() {
        return pos.x * Game.BUFF_SIZE;
    }

    public int getY() {
        return pos.y * Game.BUFF_SIZE;
    }

    public Point getPos() {
        return pos;
    }

    public ArrayList<Point> getTrail() {
        return trail;
    }

    public void clearTrail() {
        trail.clear();
    }

    private void insertTrailPlot(Point n) {
        trail.add(0, n);
        if (trail.size() > Game.TRAIL_SIZE) {
            trail.remove(Game.TRAIL_SIZE);
        }
    }

    public void updatePosition(Point p) {
        insertTrailPlot(pos);
        pos = p;
        //todo - have previous positions queued for the trail
    }
}
