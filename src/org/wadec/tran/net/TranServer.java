package org.wadec.tran.net;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;
import org.wadec.tran.game.Game;
import org.wadec.tran.game.Game.Direction;
import org.wadec.tran.game.Player;
import org.wadec.tran.game.PowerUp;
import org.wadec.tran.game.PowerUp.Type;

/**
 * outbound (server)
 *
 * @author wadec
 */
public class TranServer implements Runnable {

    public final Random random = new Random();
    private boolean open = true;
    private final ServerSocket server;
    private final ArrayList<Player> players = new ArrayList<>(),
            clr = new ArrayList<>();
    private final ArrayList<PowerUp> tokens = new ArrayList<>();
    private long gameTick = System.currentTimeMillis();

    public TranServer(int port) throws IOException {
        server = new ServerSocket(port);
        server.setSoTimeout(5);
    }

    private void gameProcessWork() {
        // here we will move our players, and detect collisions
        for (Player player : players) {
            if (player.isDead()) {
                if (player.canRespawn()) {
                    player.revive();
                    announce(7);
                    announce(player.getUid());
                    announcePosition(player, player.getPosition());
                    player.changeDirection(newDirection(player));
                }
                continue;
            }

            if (!tokens.isEmpty()) {
                PowerUp token = isOnToken(player.getPosition());
                if (token != null) {
                    handlePowerup(token, player);
                }
            }

            // did we hit a wall ?
            if (isOutOfBounds(player.getPosition())) {
                reportDead(player);
                // System.out.println("reported out of bounds!");
                continue;
            }

            if (hitATrail(player)) {
                reportDead(player);
                //System.out.println("hit a trail!");
                continue;
            }

            // todo - collision with trail & player
            Player collide = collisionHeadOn(player);
            if (collide != null) {
                reportDead(collide);
                reportDead(player);
                continue;
            }

            announcePosition(player, player.updatePosition());
        }

        // check for tokens
        // todo - make a better system for this
        if (tokens.isEmpty() || (tokens.size() < Game.MAX_POWERUPS && random.nextInt(150) == 0)) {
            PowerUp token = new PowerUp(Type.POINTS, random.nextInt(4) + 1, genSpawn());
            tokens.add(token);
            announce(20);
            announce(Type.POINTS.ordinal());
            announce(token.value);
            announce(token.position.x);
            announce(token.position.y);
        }
    }

    public void handlePowerup(PowerUp token, Player player) {
        switch (token.type) {
            case POINTS:
                player.score += token.value * Game.SCORE_MULTIPLR;
                announce(21);
                announce(player.getUid());
                tokens.remove(token);
                break;
        }
    }

    public void announcePosition(Player player, Point p) {
        announce(6);
        announce(player.getUid());
        announce(p.x);
        announce(p.y);
    }

    public void reportDead(Player player) {
        player.kill(genSpawn());
        announce(8);
        announce(player.getUid());
    }

    public boolean isOutOfBounds(Point point) {
        return point.x >= Game.BOUNDS_X || point.x <= 0 || point.y >= Game.BOUNDS_Y || point.y <= 0;
    }

    public Player collisionHeadOn(Player player) {
        Point point = player.getPosition();
        for (Player play : players) {
            if (player != play) {
                if (play.getPosition().equals(point)) {
                    return play;
                }
            }
        }
        return null;
    }

    public boolean hitATrail(Player player) {
        //Point point = player.getPosition();
        for (Player play : players) {
            if (play.beenThere(player)) {
                return true;
            }
        }
        return false;
    }

    public PowerUp isOnToken(Point p) {
        for (PowerUp pow : tokens) {
            if (pow.position.equals(p)) {
                return pow;
            }
        }
        return null;
    }

    private void gameInputWork() throws IOException {
        // here we read their input
        for (Player player : players) {
            if (player.conn.ready()) {
                switch (player.conn.stream.readInt()) {
                    case 2: //change direction
                        Direction d = Direction.values()[player.conn.stream.readInt()];
                        if (d == player.getDirection()) { // its already set
                            break;
                        }
                        if (!player.changeDirection(d)) {
                            player.conn.stream.send(12); // bad direction
                        }
                        break;
                    case 4: // lobby request
                        if (player.isHost()) {
                            newLobbySession();
                        } else {
                            player.conn.send(16, "req submitted");
                            players.get(0).conn.send(16, player.name + " - req return to lobby");
                        }
                        break;
                    case 80: // ping reply
                        player.resetPing();
                        break;
                }
            }
        }
    }

    private void lobbyWork() throws IOException, InterruptedException {
        for (Player player : players) {
            if (player.conn.ready()) {
                switch (player.conn.stream.readInt()) {
                    case 2: //message
                        send(player, 2, player.name + "\n" + player.conn.stream.readStr());
                        break;
                    case 3: //quit
                        send(player, 3);
                        clr.add(player);
                        System.out.println(player + " has quit");

                        // if host quits, change to next player
                        if (player.isHost()) {
                            Player n = players.get(1);
                            n.conn.send(18); // YOU are the host
                            send(n, 19, n.conn.getAddress().getHostAddress()); //announce the change to everyone
                        }
                        break;
                    case 5: //start game
                        if (player.isHost()) {
                            forceStartGame();
                        } else {
                            player.conn.send(19, "you are not the host");
                        }
                        break;
                    case 17: // reconnected successfully
                        send(player, 2, player.name + "\nConnection reestablished");
                        break;
                    case 80: //ping reply
                        player.resetPing();
                        break;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                //ping timer
                for (Player player : players) {
                    if (player.hasTimedOut() && !clr.contains(player)) {
                        clr.add(player);
                        System.out.println("kick " + player.name);
                    } else if (player.isDueForPing() && !player.wasPingSent()) {
                        player.sendPing();
                    }
                }

                if (!clr.isEmpty() && open) {
                    for (Player player : clr) {
                        players.remove(player);
                        announce(3, player.name);
                    }
                    announce(19, "space for " + (Game.MAX_PLAYERS - players.size()) + " players.");
                    System.out.println("Cleared " + clr.size());
                    clr.clear();
                }

                /**
                 * open = lobby activity and accepting new connections closed =
                 * game time and no new connections
                 */
                if (open) {
                    // are we ready to go yet?
                    if (players.size() >= Game.MAX_PLAYERS) {
                        forceStartGame();
                        continue;
                    }

                    // accept our new connections
                    try {
                        Connection con = new Connection(server.accept());
                        if (con.stream.readInt() != 19) {
                            con.close();
                            System.out.println("[S] invalid client code");
                            continue; //faggot
                        }
                        int v;
                        if ((v = con.stream.readInt()) != Game.GAME_VERSION) {
                            con.send(9);
                            con.send(v);
                            con.close();
                            System.out.println("[S] invalid game version (client vers: " + v + ")");
                            continue;
                        }

                        //register the user
                        String name = con.stream.readStr().replaceAll(" ", "_");
                        name = name.substring(0, name.length() > Game.MAX_NAME_LEN ? Game.MAX_NAME_LEN : name.length());
                        Player p = new Player(name, con, players.isEmpty() ? 1 : 0);

                        //check if user exists
                        if (isNickOnline(p.name)) {
                            con.send(4, "nick in use");
                            con.close();
                            continue;
                        }

                        //todo - add restrictions for nicknames
                        players.add(p);
                        p.conn.send(19, p.isHost() ? "server online" : "connected");
                        send(p, 1, p.name); //announce the new member
                        System.out.println("[S] " + p + "\tconnected");

                        //send the new user who is here
                        p.conn.stream.send(15);
                        p.conn.stream.send(players.size());
                        for (Player pl : players) {
                            p.conn.stream.send(pl.name);
                        }

                        //if the server is full, we need to start
                        if (players.size() >= Game.MAX_PLAYERS) {
                            forceStartGame();
                        }

                    } catch (SocketTimeoutException ex) {
                    }

                    // process lobby workings
                    lobbyWork();
                } else {
                    // here we process game work
                    if (System.currentTimeMillis() - gameTick > Game.LAG_TARGET) {
                        gameProcessWork();
                        gameTick = System.currentTimeMillis();
                    }
                    gameInputWork();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void newLobbySession() {
        announce(4);//end the game
        // clear all game variables
        tokens.clear();
        for (Player pl : players) {
            if (pl.hasTimedOut()) {
                clr.add(pl);
            }
        }

        // announce who is here
        announce(15);
        announce(players.size());
        for (Player pl : players) {
            announce(pl.name);

            //clear some of the stats
            pl.setUid(-1);
            pl.resetPing();
        }

        announce(19, "Game ended by host");

        open = true;
    }

    public void forceStartGame() throws InterruptedException {
        System.out.println("[S] force starting game");
        announce(5);//lets go

        Thread.sleep(50);

        /*
         * here we configure the users for game play.
         *
         * 1 - assign them a uid
         * 2 - assign them a color
         * 3 - assign them a starting position
         * 4 - establish their initial direction
         *
         */
        int id = 0;
        announce(players.size());//how many players bro
        for (Player player : players) {
            //configure
            player.setUid(id++);
            player.setColor(genColor()); //pick a random color
            player.setPosition(genSpawn()); //pick a random spot
            player.changeDirection(newDirection(player)); //update our direction to allow safe respawn

            //send
            announce(player.getUid());// uid
            announce(player.name);// name
            announce(Game.getColorId(player.getColor())); //color
            announce(player.getPosition().x);// start x
            announce(player.getPosition().y);// start y

            System.out.println("[S] Announced: " + player);
            player.resetPing();
        }
        System.out.println("[S] configured and broadcasted " + id + " players.");

        announce(30); // we are all done here
        open = false;

        //we will do a countdown here, expect lag msg
        for (int i = Game.RESPAWN_TIME; i >= 0; i--) {
            announce(14);
            announce(i);
            Thread.sleep(600);
        }
    }

    public Direction newDirection(Player player) {
        return player.getPosition().x >= (Game.BOUNDS_X / 2) + Game.FRM_OFFSET_X ? Direction.WEST : Direction.EAST;
    }

    public boolean isColorInUse(Color c) {
        for (Player player : players) {
            if (player.getColor() != null && player.getColor() == c) {
                return true;
            }
        }
        return false;
    }

    public Color genColor() {
        Color c = Game.COLOR_OPTIONS[random.nextInt(Game.COLOR_OPTIONS.length)];
        if (isColorInUse(c)) {
            return genColor();
        }
        return c;
    }

    public Point genSpawn() {
        Point p = new Point(random.nextInt(Game.BOUNDS_X - 10) + 5, random.nextInt(Game.BOUNDS_Y - 10) + 5);
        return p;
    }

    public int getQuadrant(Point p) {
        return 0;
    }

    public void announce(int op) {
        for (Player player : players) {
            player.conn.stream.send(op);
        }
    }

    public void announce(String str) {
        for (Player player : players) {
            player.conn.stream.send(str);
        }
    }

    public void announce(int op, String str) {
        for (Player player : players) {
            player.conn.stream.send(op);
            player.conn.stream.send(str);
        }
    }

    public void announce(String[] str) {
        for (Player player : players) {
            for (String s : str) {
                player.conn.stream.send(s);
            }
        }
    }

    public void send(Player source, int op) {
        send(source, op, null);
    }

    public void send(Player source, int op, String pack) {
        for (Player player : players) {
            if (player != source) {
                if (pack != null) {
                    player.conn.send(op, pack);
                } else {
                    player.conn.stream.send(op);
                }
            }
        }
    }

    public boolean isNickOnline(String nick) {
        for (Player play : players) {
            if (play.name.equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }
}
