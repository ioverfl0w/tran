package org.wadec.tran.game;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import org.wadec.tran.ui.GFrame;

/**
 *
 * @author wadec
 */
public class Game {

    public final GFrame frame;
    public final GameCore core;
    private final ArrayList<Profile> players = new ArrayList<>();
    private final ArrayList<PowerUp> powerups = new ArrayList<>();

    public Game(GameCore c) {
        core = c;
        frame = new GFrame(this);
    }

    public void addPowerup(PowerUp p) {
        powerups.add(p);
    }

    public void remPowerup(PowerUp p) {
        powerups.remove(p);
    }

    public ArrayList<PowerUp> getPowerups() {
        return powerups;
    }

    public PowerUp getPowerup(Point p) {
        for (PowerUp power : powerups) {
            if (power.position.equals(p)) {
                return power;
            }
        }
        return null;
    }

    public ArrayList<Profile> getPlayers() {
        return players;
    }

    public Profile getPlayer(int uid) {
        return players.get(uid);
    }

    public Profile getPlayer(String name) {
        for (Profile player : players) {
            if (player.name.equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    public int addUser(String nick, Cart c) {
        Profile p = new Profile(nick, c);
        players.add(p);
        return players.indexOf(p);
    }

    public void userQuit(String usr) {
        Profile p = getPlayer(usr);
        p.cart.score = -9999;
        p.isDead = true;
    }

    public enum Direction {

        NORTH, EAST, SOUTH, WEST
    }

    public static int getColorId(Color c) {
        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            if (COLOR_OPTIONS[i] == c) {
                return i;
            }
        }
        return -1;
    }

    // todo - have some options be customizable per lobby
    public static final int PORT = 24564;
    public static final int MAX_PLAYERS = 4;
    public static final int BOUNDS_X = 50, BOUNDS_Y = 30; // Frame is 700x300 , leave 200 on x for stats
    public static final int FRM_OFFSET_X = 5, FRM_OFFSET_Y = 50;
    public static final int RESPAWN_TIME = 4; // in seconds
    public static final int LAG_TARGET = 100; // 100ms ?
    public static final int CART_SIZE = 5; // radius
    public static final int BUFF_SIZE = 10; // size between points
    public static final int SCORE_MULTIPLR = 100;
    public static final int GAME_VERSION = 2;
    public static final int PING_TIME = 5; // in seconds
    public static final int MESSAGE_TIME = 5; // in seconds
    public static final int TRAIL_SIZE = 24; // todo - make this adjustable (ie power ups)
    public static final int MAX_NAME_LEN = 10;
    public static final int MAX_POWERUPS = 5;
    public static final Color[] COLOR_OPTIONS = new Color[]{
        Color.BLUE,
        Color.RED,
        Color.PINK,
        Color.ORANGE,};
}
