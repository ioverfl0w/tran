package org.wadec.tran.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 *
 * @author wadec
 */
public class NetStream {

    private final PrintStream out;
    private final BufferedReader in;

    public NetStream(Socket socket) throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintStream(socket.getOutputStream());
    }
    
    public boolean isWaiting() throws IOException {
        return in.ready();
    }

    public int readInt() throws IOException {
        return in.read();
    }

    public String readStr() throws IOException {
        return in.readLine();
    }

    public void send(int i) {
        out.write(i);
        out.flush();
    }

    public void send(String str) {
        out.println(str);
        out.flush();
    }
}
