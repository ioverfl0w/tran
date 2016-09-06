package org.wadec.tran.game;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import org.wadec.tran.game.Game.Direction;
import org.wadec.tran.net.Connection;

/**
 *
 * @author wadec
 */
public class Player {

    public final String name;
    public final Connection conn;
    public int score = 0;
    private boolean isDead = false;
    private long deadTime = 0;
    private long lastPingResp = System.currentTimeMillis();
    private boolean pingSent = false;
    private final ArrayList<Point> trail = new ArrayList<>();
    private final int rights; //1 = host, 0 = player
    private int uid = -1;
    private Color color = Color.BLACK;
    private Point position = new Point(0, 0);
    private Direction direction = Direction.NORTH;
    private long lagTime = 0; // time in ms

    public Player(String n, Connection c, int r) {
        name = n;
        conn = c;
        rights = r;
    }

    public boolean isDueForPing() {
        return !pingSent && ((System.currentTimeMillis() - lastPingResp) >= Game.PING_TIME * 1000);
    }

    public boolean hasTimedOut() {
        return pingSent && ((System.currentTimeMillis() - lastPingResp) >= (Game.PING_TIME * 1000) * 2);
    }

    public void resetPing() {
        lagTime = System.currentTimeMillis() - lastPingResp;
        lastPingResp = System.currentTimeMillis();
        pingSent = false;
    }

    public boolean wasPingSent() {
        return pingSent;
    }

    public long getLagTime() {
        return lagTime;
    }

    public void sendPing() {
        conn.send(80);
        pingSent = true;
        lastPingResp = System.currentTimeMillis();
    }

    public boolean beenThere(Player player) {
        Point p = player.position;
        for (Point po : trail) {
            if ((po.equals(p) && player == this && !po.equals(trail.get(0))) || (po.equals(p) && player != this)) {
                return true;
            }
        }
        return false;
    }

    public void kill(Point n) {
        position = n;
        score -= (1 * Game.SCORE_MULTIPLR);
        isDead = true;
        trail.clear();
        deadTime = System.currentTimeMillis();
    }

    public boolean isDead() {
        return isDead;
    }

    public void revive() {
        isDead = false;
        trail.clear();
    }

    public boolean canRespawn() {
        return System.currentTimeMillis() - deadTime >= Game.RESPAWN_TIME * 1000;
    }

    public boolean changeDirection(Direction neu) {
        switch (neu) {
            case NORTH:
                if (direction == Direction.SOUTH) {
                    return false;
                }
                break;
            case EAST:
                if (direction == Direction.WEST) {
                    return false;
                }
                break;
            case SOUTH:
                if (direction == Direction.NORTH) {
                    return false;
                }
                break;
            case WEST:
                if (direction == Direction.EAST) {
                    return false;
                }
                break;
        }
        direction = neu;
        return true;
    }

    private void insertTrailPlot(Point n) {
        trail.add(0, n);
        if (trail.size() > Game.TRAIL_SIZE) {
            trail.remove(Game.TRAIL_SIZE);
        }
    }

    public Point updatePosition() {
        insertTrailPlot(new Point(position)); // insert the previous as the first plot
        switch (direction) {
            case NORTH:
                position.setLocation(position.x, position.y - 1);
                break;
            case EAST:
                position.setLocation(position.x + 1, position.y);
                break;
            case SOUTH:
                position.setLocation(position.x, position.y + 1);
                break;
            case WEST:
                position.setLocation(position.x - 1, position.y);
                break;
        }
        return position;
    }

    public Direction getDirection() {
        return direction;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point p) {
        position = p;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;
    }

    public boolean isHost() {
        return rights == 1;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int id) {
        uid = id;
    }

    @Override
    public String toString() {
        return "[User:" + name + ";UID:" + getUid() + ";Addr:" + conn + ";Color:" + color.toString() + ";IsHost:" + isHost() + "]";
    }
}
