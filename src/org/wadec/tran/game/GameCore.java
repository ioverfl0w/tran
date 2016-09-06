package org.wadec.tran.game;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import org.wadec.tran.Engine;
import org.wadec.tran.game.PowerUp.Type;
import org.wadec.tran.ui.LobbyFrame;
import org.wadec.tran.ui.MainMenu;

/**
 * outbound (client side)
 *
 * @author wadec
 */
public class GameCore implements Runnable {

    public final Engine engine;
    public final Player player;//this is us
    private LobbyFrame lobby = null;
    private Game game = null;
    private boolean endSession = false;

    public GameCore(Engine e, Player p) {
        engine = e;
        player = p;
        lobby = new LobbyFrame(this);
    }

    private void gameWork() throws IOException {
        switch (player.conn.stream.readInt()) {
            case 3: //quit
                game.userQuit(player.conn.stream.readStr());
                break;
            case 4: // return to lobby
                lobby = new LobbyFrame(this);
                game.frame.dispose();
                game = null;
                return;
            case 6: // update a player's position
                game.getPlayer(player.conn.stream.readInt()).cart
                        .updatePosition(
                                new Point(player.conn.stream.readInt(),
                                        player.conn.stream.readInt()));
                break;
            case 7: // player revived
                revivePlayer(game.getPlayer(player.conn.stream.readInt()));
                break;
            case 8: // player died
                killPlayer(game.getPlayer(player.conn.stream.readInt()));
                break;
            case 12: // bad direction
                game.frame.badMove();
                break;
            case 14: // count down numbers
                game.frame.countDown(player.conn.stream.readInt());
                break;
            case 16: // on screen message (one at a time)
                game.frame.postMessage(player.conn.stream.readStr());
                break;
            case 18: // we are the host now, let's set up shop
                
                break;
            case 20: // announced power up
                game.addPowerup(new PowerUp(Type.values()[player.conn.stream.readInt()],
                        player.conn.stream.readInt(),
                        new Point(player.conn.stream.readInt(),
                                player.conn.stream.readInt())));
                break;
            case 21: // power up fetched
                int uid = player.conn.stream.readInt();
                PowerUp p = game.getPowerup(game.getPlayer(uid).cart.getPos());
                game.getPlayer(uid).cart.score += p.value * Game.SCORE_MULTIPLR;
                game.remPowerup(p);
                break;
            case 80: // ping pong
                player.conn.send(80);
                break;
        }
        game.frame.repaint();
    }

    private void killPlayer(Profile player) {
        player.cart.score -= (1 * Game.SCORE_MULTIPLR);
        player.cart.clearTrail();
        player.isDead = true;
        game.frame.repaint();
    }

    private void revivePlayer(Profile player) {
        player.isDead = false;
        game.frame.repaint();
    }

    private void createGame() throws IOException, InterruptedException {
        game = new Game(this);
        Thread.sleep(50);
        /*
         * we need each player's: 
         * a - player id 
         * b - their name 
         * c - their color
         * d - starting position
         */
        int len = player.conn.stream.readInt();
        for (int i = 0; i < len; i++) {
            int id = player.conn.stream.readInt();
            String nk = player.conn.stream.readStr();
            Color cl = Game.COLOR_OPTIONS[player.conn.stream.readInt()];
            Point sp = new Point(player.conn.stream.readInt(), player.conn.stream.readInt());
            if (game.addUser(nk, new Cart(sp, cl)) != id) {
                System.out.println("Error matching ids");
            } else {
                System.out.println("added: " + nk + " (" + id + ") " + sp);
            }
        }

        if (player.conn.stream.readInt() != 30) {
            System.out.println("Something went wrong - we're not ready");
        }
        System.out.println("Session loaded - ready to play");
    }

    private void lobbyWork() throws IOException, InterruptedException {
        switch (player.conn.stream.readInt()) {
            case 1: //join
                String u = player.conn.stream.readStr();
                lobby.print(u + " has joined.");
                lobby.adjustList(true, u);
                break;
            case 2: //message
                lobby.print("[" + player.conn.stream.readStr() + "] " + player.conn.stream.readStr());
                break;
            case 3: //quit
                String w = player.conn.stream.readStr();
                lobby.print(w + " has quit.");
                lobby.adjustList(false, w);
                break;
            case 4: //error quit something
                // todo- handle these better
                System.exit(0);
                break;
            case 5: //start the game
                lobby.dispose();
                createGame();
                lobby = null;
                break;
            case 9: // bad version
                lobby.dispose();
                lobby = null;
                new MainMenu(engine, "Bad version - v" + player.conn.stream.readInt() + " required");
                endSession = true;
                break;
            case 15: //lobby user list
                int i = player.conn.stream.readInt();
                for (int x = 0; x < i; x++) {
                    lobby.adjustList(true, player.conn.stream.readStr());
                }
                break;
            case 19: //serv msg
                lobby.print("[server] " + player.conn.stream.readStr());
                break;
            case 80: // ping
                player.conn.send(80);
                break;
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (player.conn.ready()) {
                    //we are still in the lobby
                    if (lobby != null) {
                        lobbyWork();
                        continue;
                    }
                    
                    //check if we need to close out
                    if (endSession)
                        return;
                    
                    //here we do game work
                    gameWork();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
