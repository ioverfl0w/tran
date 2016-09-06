package org.wadec.tran.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import org.wadec.tran.io.NetStream;

/**
 *
 * @author wadec
 */
public class Connection {

    private final Socket socket;
    public final NetStream stream;

    public Connection(Socket sock) throws IOException {
        socket = sock;
        stream = new NetStream(socket);
    }
    
    public InetAddress getAddress() {
        return socket.getInetAddress();
    }
    
    public void send(int op) {
        stream.send(op);
    }
    
    public void send(int op, String pack) {
        stream.send(op);
        stream.send(pack);
    }
    
    public boolean ready() throws IOException {
        return stream.isWaiting();
    }

    public void close() {
        try {
            stream.send(4);
            socket.close();
        } catch (Exception ex) {
        }
    }
    
    @Override
    public String toString() {
        return "Addr:" + socket.getInetAddress().getHostAddress();
    }
}
