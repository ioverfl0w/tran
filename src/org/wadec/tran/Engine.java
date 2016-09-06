package org.wadec.tran;

import java.io.IOException;
import java.net.Socket;
import org.wadec.tran.game.Game;
import org.wadec.tran.game.GameCore;
import org.wadec.tran.game.Player;
import org.wadec.tran.net.Connection;
import org.wadec.tran.net.TranServer;
import org.wadec.tran.ui.MainMenu;

/**
 *
 * @author wadec
 */
public class Engine {

    private TranServer host = null;
    private GameCore core;

    public boolean isHost() {
        return host != null;
    }

    public void createSession(String user, String server) throws IOException {
        Connection con;
        if (server.equals("")) { // we need to start a server
            host = new TranServer(Game.PORT);
            new Thread(host).start();

            //establish the connection
            con = create(new Socket("localhost", Game.PORT));
        } else {
            //establish the connection
            con = create(new Socket(server, Game.PORT));
        }
        con.stream.send(user);

        core = new GameCore(this, new Player(user, con, 1));

        new Thread(core).start();
    }

    private Connection create(Socket socket) throws IOException {
        Connection c = new Connection(socket);
        c.stream.send(19);
        c.stream.send(Game.GAME_VERSION);
        return c;
    }

    public static void main(String[] args) {
        new MainMenu(new Engine());
    }
}
